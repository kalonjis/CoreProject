package be.steby.CoreProject.bll.domains.auth.services.twofactor.backupcodetwofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.*;
import be.steby.CoreProject.bll.domains.auth.services.twofactor.config.BackupCodeConfiguration;
import be.steby.CoreProject.dal.repositories.TwoFactorAuthRepository;
import be.steby.CoreProject.dl.entities.TwoFactorAuth;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of BackupCodeTwoFactorService for managing backup code 2FA.
 * 
 * This service handles the generation, verification, and management of backup codes.
 * Backup codes are stored as hashed JSON array in the database for security.
 * 
 * Security features:
 * - Codes are bcrypt-hashed before storage
 * - Each code can only be used once
 * - Failed attempts are tracked for rate limiting
 * - Constant-time comparison to prevent timing attacks
 * 
 * Code format: 16-character codes formatted as XXXX-XXXX-XXXX-XXXX
 * Storage format: JSON array of hashed codes ["$2a$10$...", "$2a$10$...", ...]
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackupCodeTwoFactorServiceImpl implements BackupCodeTwoFactorService {

    private final TwoFactorAuthRepository twoFactorAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final BackupCodeGenerator backupCodeGenerator;
    private final BackupCodeConfiguration config;

    @Override
    @Transactional
    public List<String> generateBackupCodes(User user) {
        log.info("Generating backup codes for user: {}", user.getUsername());
        
        // Ensure user has at least one other 2FA method enabled
        if (!hasOtherTwoFactorMethodEnabled(user)) {
            throw new IllegalStateException("User must have at least one other 2FA method enabled before generating backup codes");
        }
        
        // Generate plain text codes using generator
        List<String> plainCodes = backupCodeGenerator.generateCodes(user);
        
        // Hash the codes for secure storage
        List<String> hashedCodes = hashCodes(plainCodes);
        
        // Store in database
        TwoFactorAuth backupCodeAuth = getOrCreateBackupCodeAuth(user);
        backupCodeAuth.setBackupCodesHashed(serializeHashedCodes(hashedCodes));
        backupCodeAuth.setRemainingBackupCodes(config.getCount());
        backupCodeAuth.setEnabled(true);
        backupCodeAuth.setEnabledAt(Instant.now());
        backupCodeAuth.setFailedAttempts(0);
        backupCodeAuth.setLockedUntil(null);
        
        twoFactorAuthRepository.save(backupCodeAuth);
        
        log.info("Generated {} backup codes for user: {}", config.getCount(), user.getUsername());
        
        return plainCodes;
    }

    @Override
    @Transactional
    public boolean verifyAndConsumeBackupCode(User user, String providedCode) {
        log.debug("Verifying backup code for user: {}", user.getUsername());
        
        // Clean the provided code (remove spaces, dashes)
        String cleanCode = cleanBackupCode(providedCode);
        
        Optional<TwoFactorAuth> backupAuthOpt = twoFactorAuthRepository
                .findByUserAndTypeAndEnabledTrue(user, TwoFactorType.BACKUP_CODES);
        
        if (backupAuthOpt.isEmpty()) {
            log.warn("User {} attempted to use backup code but backup codes are not enabled", user.getUsername());
            throw new BackupCodeNotEnabledException("Backup codes are not enabled for this user");
        }
        
        TwoFactorAuth backupAuth = backupAuthOpt.get();
        
        // Check if account is locked
        if (backupAuth.isLocked()) {
            log.warn("User {} attempted to use backup code but account is locked", user.getUsername());
            throw new BackupCodeInvalidException("Too many failed attempts. Please try again later.");
        }
        
        // Check if any codes remain
        if (backupAuth.getRemainingBackupCodes() <= 0) {
            log.warn("User {} attempted to use backup code but no codes remain", user.getUsername());
            throw new BackupCodeExhaustedException("All backup codes have been used");
        }
        
        // Get hashed codes from database
        List<String> hashedCodes = deserializeHashedCodes(backupAuth.getBackupCodesHashed());
        
        // Find matching code and remove it
        boolean codeFound = false;
        List<String> remainingHashedCodes = new ArrayList<>();
        
        for (String hashedCode : hashedCodes) {
            if (!codeFound && passwordEncoder.matches(cleanCode, hashedCode)) {
                codeFound = true;
                log.debug("Backup code matched and will be consumed for user: {}", user.getUsername());
                // Don't add this code to remaining codes (consume it)
            } else {
                remainingHashedCodes.add(hashedCode);
            }
        }
        
        if (codeFound) {
            // Update the database with remaining codes
            backupAuth.setBackupCodesHashed(serializeHashedCodes(remainingHashedCodes));
            backupAuth.setRemainingBackupCodes(remainingHashedCodes.size());
            backupAuth.recordSuccessfulVerification();
            
            twoFactorAuthRepository.save(backupAuth);
            
            log.info("Backup code successfully used by user: {}. Remaining codes: {}", 
                    user.getUsername(), remainingHashedCodes.size());
            
            return true;
        } else {
            log.warn("Invalid backup code provided by user: {}", user.getUsername());
            recordFailedAttempt(backupAuth);
            throw new BackupCodeInvalidException("Invalid backup code");
        }
    }

    @Override
    public int getRemainingBackupCodesCount(User user) {
        return twoFactorAuthRepository
                .findByUserAndTypeAndEnabledTrue(user, TwoFactorType.BACKUP_CODES)
                .map(TwoFactorAuth::getRemainingBackupCodes)
                .orElse(0);
    }

    @Override
    public boolean hasBackupCodesAvailable(User user) {
        return getRemainingBackupCodesCount(user) > 0;
    }

    @Override
    @Transactional
    public List<String> regenerateBackupCodes(User user) {
        log.info("Regenerating backup codes for user: {}", user.getUsername());
        
        // This is essentially the same as generateBackupCodes
        // but we explicitly mention it's a regeneration for audit purposes
        return generateBackupCodes(user);
    }

    @Override
    @Transactional
    public void disableBackupCodes(User user) {
        log.info("Disabling backup codes for user: {}", user.getUsername());
        
        twoFactorAuthRepository
                .findByUserAndTypeAndEnabledTrue(user, TwoFactorType.BACKUP_CODES)
                .ifPresent(backupAuth -> {
                    backupAuth.setEnabled(false);
                    backupAuth.setDisabledAt(Instant.now());
                    backupAuth.setRemainingBackupCodes(0);
                    twoFactorAuthRepository.save(backupAuth);
                    log.info("Backup codes disabled for user: {}", user.getUsername());
                });
    }

    @Override
    public boolean isBackupCodesEnabled(User user) {
        return twoFactorAuthRepository
                .existsByUserAndTypeAndEnabledTrue(user, TwoFactorType.BACKUP_CODES);
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    /**
     * Clean backup code by removing spaces and dashes.
     * Users might type "A2B4-C6D8-E9F2-G4H6" but we store "A2B4C6D8E9F2G4H6"
     */
    private String cleanBackupCode(String code) {
        if (code == null) return "";
        return code.replaceAll("[\\s-]", "").toUpperCase();
    }

    /**
     * Hash a list of plain text codes.
     */
    private List<String> hashCodes(List<String> plainCodes) {
        return plainCodes.stream()
                .map(code -> passwordEncoder.encode(cleanBackupCode(code)))
                .toList();
    }

    /**
     * Serialize hashed codes to JSON string for database storage.
     */
    private String serializeHashedCodes(List<String> hashedCodes) {
        try {
            return objectMapper.writeValueAsString(hashedCodes);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize hashed backup codes", e);
            throw new RuntimeException("Failed to serialize backup codes", e);
        }
    }

    /**
     * Deserialize hashed codes from JSON string.
     */
    private List<String> deserializeHashedCodes(String hashedCodesJson) {
        try {
            if (hashedCodesJson == null || hashedCodesJson.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(hashedCodesJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize hashed backup codes", e);
            throw new RuntimeException("Failed to deserialize backup codes", e);
        }
    }

    /**
     * Get existing backup code auth or create new one.
     */
    private TwoFactorAuth getOrCreateBackupCodeAuth(User user) {
        return twoFactorAuthRepository
                .findByUserAndType(user, TwoFactorType.BACKUP_CODES)
                .orElse(TwoFactorAuth.builder()
                        .user(user)
                        .type(TwoFactorType.BACKUP_CODES)
                        .enabled(false)
                        .isPrimary(false)
                        .build());
    }

    /**
     * Check if user has at least one other 2FA method enabled.
     */
    private boolean hasOtherTwoFactorMethodEnabled(User user) {
        return twoFactorAuthRepository
                .findByUserAndEnabledTrue(user)
                .stream()
                .anyMatch(auth -> auth.getType() != TwoFactorType.BACKUP_CODES);
    }

    /**
     * Record a failed attempt and handle locking if necessary.
     */
    private void recordFailedAttempt(TwoFactorAuth backupAuth) {
        backupAuth.recordFailedAttempt();
        
        // Lock after configured failed attempts for configured duration
        if (backupAuth.getFailedAttempts() >= config.getSecurity().getMaxFailedAttempts()) {
            long lockoutSeconds = config.getSecurity().getLockoutDurationMinutes() * 60L;
            backupAuth.setLockedUntil(Instant.now().plusSeconds(lockoutSeconds));
            
            log.warn("Backup codes locked for user {} due to {} failed attempts", 
                    backupAuth.getUser().getUsername(), 
                    config.getSecurity().getMaxFailedAttempts());
        }
        
        twoFactorAuthRepository.save(backupAuth);
    }
}