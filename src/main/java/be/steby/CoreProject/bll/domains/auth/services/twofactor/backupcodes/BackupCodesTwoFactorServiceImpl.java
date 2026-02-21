package be.steby.CoreProject.bll.domains.auth.services.twofactor.backupcodes;

import be.steby.CoreProject.bll.domains.auth.events.TwoFactorDisabledEvent;
import be.steby.CoreProject.bll.domains.auth.events.TwoFactorEnabledEvent;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.BackupCodesTwoFactorAlreadyEnabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.BackupCodesTwoFactorNotEnabledException;
import be.steby.CoreProject.bll.domains.auth.models.BackupCodesSetupResult;
import be.steby.CoreProject.bll.domains.auth.services.twofactor.config.BackupCodesConfiguration;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.Device;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import be.steby.CoreProject.dal.repositories.TwoFactorAuthRepository;
import be.steby.CoreProject.dl.entities.TwoFactorAuth;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of BackupCodesTwoFactorService.
 *
 * Focuses on generation for now as requested.
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackupCodesTwoFactorServiceImpl implements BackupCodesTwoFactorService {

    private final BackupCodesConfiguration config;
    private final BackupCodesPasswordGenerator passwordGenerator;
    private final TwoFactorAuthRepository twoFactorAuthRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Override
    @Transactional
    public BackupCodesSetupResult enable() {
        User user = userService.getAuthenticatedUser();
        log.info("Enabling backup codes 2FA for user: {}", user.getUsername());

        // Check if backup codes 2FA already exists and is enabled
        Optional<TwoFactorAuth> existingBackupCodes = twoFactorAuthRepository
                .findByUserAndTypeAndEnabledTrue(user, TwoFactorType.BACKUP_CODES);

        if (existingBackupCodes.isPresent()) {
            log.warn("Attempted to enable backup codes 2FA for user: {} but it's already enabled", user.getUsername());
            throw new BackupCodesTwoFactorAlreadyEnabledException("Backup codes two-factor authentication is already enabled");
        }

        // Generate backup codes
        List<String> backupCodes = passwordGenerator.generateBackupCodes();
        log.debug("Generated {} backup codes for user: {}", backupCodes.size(), user.getUsername());

        // Hash each backup code with BCrypt and store as semicolon-separated string
        String hashedCodes = backupCodes.stream()
                .map(passwordEncoder::encode)
                .collect(Collectors.joining(";"));

        log.debug("Hashed {} backup codes for user: {}", backupCodes.size(), user.getUsername());

        // Demote any existing primary 2FA method
        demoteExistingPrimaryTwoFactor(user);

        // Check if a disabled backup codes configuration already exists
        Optional<TwoFactorAuth> existingDisabledBackupCodes = twoFactorAuthRepository
                .findByUserAndType(user, TwoFactorType.BACKUP_CODES);

        TwoFactorAuth backupCodesAuth;
        if (existingDisabledBackupCodes.isPresent()) {
            // Reactivate existing configuration with new codes
            backupCodesAuth = existingDisabledBackupCodes.get();
            backupCodesAuth.setBackupCodesHashed(hashedCodes);
            backupCodesAuth.setRemainingBackupCodes(backupCodes.size());
            backupCodesAuth.setEnabled(true);
            backupCodesAuth.setDisabledAt(null);
            backupCodesAuth.setEnabledAt(Instant.now());
            log.debug("Reactivated existing backup codes 2FA configuration for user: {}", user.getUsername());
        } else {
            // Create new backup codes 2FA configuration
            backupCodesAuth = TwoFactorAuth.builder()
                    .user(user)
                    .type(TwoFactorType.BACKUP_CODES)
                    .backupCodesHashed(hashedCodes)
                    .remainingBackupCodes(backupCodes.size())
                    .enabled(true)
                    .isPrimary(true)
                    .enabledAt(Instant.now())
                    .failedAttempts(0)
                    .label("Backup recovery codes")
                    .build();
            log.debug("Created new backup codes 2FA configuration for user: {}", user.getUsername());
        }

        // Set as primary and save
        backupCodesAuth.setIsPrimary(false);
        twoFactorAuthRepository.save(backupCodesAuth);
        log.info("Backup codes 2FA enabled successfully for user: {}", user.getUsername());

        // Publish activity log event
        Device device = deviceService.detectCurrentDevice();
        eventPublisher.publishEvent(new TwoFactorEnabledEvent(user, device, TwoFactorType.BACKUP_CODES));

        return new BackupCodesSetupResult(backupCodes);
    }

    @Override
    @Transactional
    public void disable() {
        User user = userService.getAuthenticatedUser();
        log.info("Disabling backup codes 2FA for user: {}", user.getUsername());

        TwoFactorAuth backupCodesAuth = twoFactorAuthRepository
                .findByUserAndTypeAndEnabledTrue(user, TwoFactorType.BACKUP_CODES)
                .orElseThrow(() -> {
                    log.warn("Attempted to disable backup codes 2FA for user {} but it's not enabled", user.getUsername());
                    return new BackupCodesTwoFactorNotEnabledException("Backup codes two-factor authentication is not enabled");
                });

        // Disable the configuration (don't delete for audit purposes)
        backupCodesAuth.setEnabled(false);
        backupCodesAuth.setIsPrimary(false);
        backupCodesAuth.setDisabledAt(Instant.now());

        twoFactorAuthRepository.save(backupCodesAuth);
        log.info("Backup codes 2FA disabled successfully for user: {}", user.getUsername());

        // Publish activity log event
        Device device = deviceService.detectCurrentDevice();
        eventPublisher.publishEvent(new TwoFactorDisabledEvent(user, device, TwoFactorType.BACKUP_CODES));
    }

    @Override
    public String generateCode(User user) {
        log.debug("Generate code called for backup codes (user: {})", user.getUsername());

        // Backup codes don't generate dynamic codes like TOTP or EMAIL
        // They use pre-generated static codes
        return "";
    }

    @Override
    public boolean verifyCode(User user, String providedCode) {
        log.debug("Verifying backup code for user: {}", user.getUsername());

        TwoFactorAuth backupCodesAuth = twoFactorAuthRepository
                .findByUserAndTypeAndEnabledTrue(user, TwoFactorType.BACKUP_CODES)
                .orElseThrow(() -> {
                    log.warn("Attempted to verify backup code for user {} but backup codes 2FA is not enabled", user.getUsername());
                    return new BackupCodesTwoFactorNotEnabledException("Backup codes two-factor authentication is not enabled");
                });

        String hashedCodes = backupCodesAuth.getBackupCodesHashed();
        if (hashedCodes == null || hashedCodes.isEmpty()) {
            log.warn("No backup codes found for user: {}", user.getUsername());
            return false;
        }

        // Split hashed codes and try to match provided code
        String[] hashArray = hashedCodes.split(";");
        List<String> hashList = new ArrayList<>(Arrays.asList(hashArray));

        for (int i = 0; i < hashList.size(); i++) {
            String hash = hashList.get(i);
            if (passwordEncoder.matches(providedCode, hash)) {
                log.info("✅ Backup code verified successfully for user: {}", user.getUsername());

                // Remove the used hash from the list
                hashList.remove(i);

                // Update the stored hashes and remaining count
                String updatedHashes = String.join(";", hashList);
                backupCodesAuth.setBackupCodesHashed(updatedHashes);
                backupCodesAuth.setRemainingBackupCodes(hashList.size());
                twoFactorAuthRepository.save(backupCodesAuth);

                log.debug("Backup code used and removed for user: {}. Remaining codes: {}",
                        user.getUsername(), hashList.size());

                return true;
            }
        }

        log.warn("❌ Backup code verification failed for user: {} - no matching codes found", user.getUsername());
        return false;
    }

    /**
     * Demote existing primary 2FA method to secondary.
     */
    private void demoteExistingPrimaryTwoFactor(User user) {
        Optional<TwoFactorAuth> existingPrimary = twoFactorAuthRepository
                .findByUserAndIsPrimaryTrue(user);

        if (existingPrimary.isPresent()) {
            log.debug("Demoting existing primary 2FA method: {} for user: {}",
                    existingPrimary.get().getType(), user.getUsername());
            existingPrimary.get().setIsPrimary(false);
            twoFactorAuthRepository.save(existingPrimary.get());
        }
    }
}