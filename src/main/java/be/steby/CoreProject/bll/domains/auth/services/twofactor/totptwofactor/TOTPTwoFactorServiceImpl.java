package be.steby.CoreProject.bll.domains.auth.services.twofactor.totptwofactor;

import be.steby.CoreProject.bll.domains.auth.events.TwoFactorDisabledEvent;
import be.steby.CoreProject.bll.domains.auth.events.TwoFactorEnabledEvent;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.InvalidVerificationCodeException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.TOTPTwoFactorAlreadyEnabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.TOTPTwoFactorNotEnabledException;
import be.steby.CoreProject.bll.domains.auth.models.TotpActivationInitiateResult;
import be.steby.CoreProject.bll.domains.auth.models.TotpTwoFactorActivationBllRequest;
import be.steby.CoreProject.bll.domains.auth.services.twofactor.config.TOTPConfiguration;
import be.steby.CoreProject.bll.domains.auth.services.twofactor.config.TOTPSecretEncryptionService;
import be.steby.CoreProject.bll.domains.auth.services.twofactor.jwt.TwoFactorJwtService;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.common.exceptions.MaxAttemptsReachedException;
import be.steby.CoreProject.dal.repositories.TwoFactorAuthRepository;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.TwoFactorAuth;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

/**
 * Implementation of TOTPTwoFactorService for managing TOTP-based 2FA.
 *
 * This service handles the complete lifecycle of TOTP-based two-factor authentication:
 * - Enabling and disabling TOTP 2FA for users
 * - Generating secret keys and QR codes for authenticator app setup
 * - Verifying user-provided TOTP codes using RFC 6238 algorithm
 *
 * TOTP is the most secure 2FA method because it:
 * - Works offline (no network dependency)
 * - Is phishing-resistant
 * - Uses time-based rotating codes
 * - Is standardized (RFC 6238)
 * - Compatible with all major authenticator apps
 *
 * Security implementation:
 * - Uses cryptographically secure random for secret generation
 * - Implements RFC 6238 with HMAC-SHA1, 30-second periods, 6-digit codes
 * - Supports time window tolerance (±1 period) for slight clock differences
 * - Secrets are encrypted when stored in database
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TOTPTwoFactorServiceImpl implements TOTPTwoFactorService {

    private final TwoFactorAuthRepository twoFactorAuthRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TOTPConfiguration totpConfig;
    private final TOTPSecretEncryptionService encryptionService;
    private final TwoFactorJwtService twoFactorJwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final TotpTwoFactorActivationAttemptService totpActivationAttemptService;
    private final TotpTwoFactorVerificationAttemptService totpVerificationAttemptService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final DeviceService deviceService;

    @Autowired
    private HttpServletRequest httpServletRequest;


    // Base32 alphabet for secret encoding (RFC 4648)
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    @Override
    @Transactional
    public TOTPSetupInfo enable() {
        User user = userService.getAuthenticatedUser();
        log.info("Enabling TOTP 2FA for user: {}", user.getUsername());

        // Check if TOTP 2FA is already enabled
        if (twoFactorAuthRepository.existsByUserAndTypeAndEnabledTrue(user, TwoFactorType.TOTP)) {
            log.warn("TOTP 2FA already enabled for user: {}", user.getUsername());
            throw new TOTPTwoFactorAlreadyEnabledException("TOTP two-factor authentication is already enabled");
        }

        // Generate a new secret key
        String secretKey = generateSecretKey();

        // Check if a disabled TOTP 2FA configuration already exists
        Optional<TwoFactorAuth> existingTotpTwoFactor = twoFactorAuthRepository
                .findByUserAndType(user, TwoFactorType.TOTP);

        TwoFactorAuth totpAuth;
        if (existingTotpTwoFactor.isPresent()) {
            // Reactivate existing configuration with new secret
            totpAuth = existingTotpTwoFactor.get();
            totpAuth.setSecret(encryptionService.encrypt(secretKey)); // Encrypt the secret
            totpAuth.setEnabled(true);
            totpAuth.setDisabledAt(null);
            log.debug("Reactivated existing TOTP 2FA configuration for user: {}", user.getUsername());
        } else {
            // Create new TOTP 2FA configuration
            totpAuth = TwoFactorAuth.builder()
                    .user(user)
                    .type(TwoFactorType.TOTP)
                    .secret(encryptionService.encrypt(secretKey)) // Encrypt the secret
                    .enabled(true)
                    .isPrimary(true) // Set as primary by default
                    .enabledAt(Instant.now())
                    .build();
            log.debug("Created new TOTP 2FA configuration for user: {}", user.getUsername());
        }

        // Demote any existing primary 2FA method
        demoteExistingPrimaryTwoFactor(user);

        // Set this as primary
        totpAuth.setIsPrimary(true);
        twoFactorAuthRepository.save(totpAuth);

        // Generate setup information for the user
        String qrCodeUri = generateQRCodeUri(user.getEmail(), secretKey);
        String manualEntryKey = formatSecretForManualEntry(secretKey);

        log.info("TOTP 2FA enabled successfully for user: {}", user.getUsername());

        return new TOTPSetupInfo(secretKey, qrCodeUri, manualEntryKey);
    }

    @Override
    @Transactional
    public TotpActivationInitiateResult initiateActivation() {
        User user = userService.getAuthenticatedUser();
        log.info("Initiating TOTP 2FA activation for user: {}", user.getUsername());

        // Rate limiting check for activation attempts
        if (totpActivationAttemptService.hasExceededAttempts(user)) {
            log.warn("User {} exceeded TOTP 2FA activation attempts", user.getUsername());
            throw new MaxAttemptsReachedException("Too many TOTP 2FA activation attempts. Please try again later.");
        }

        // Validate TOTP 2FA not already enabled
        if (twoFactorAuthRepository.existsByUserAndTypeAndEnabledTrue(user, TwoFactorType.TOTP)) {
            log.warn("TOTP 2FA already enabled for user: {}", user.getUsername());
            throw new TOTPTwoFactorAlreadyEnabledException("TOTP two-factor authentication is already enabled");
        }

        // Record activation attempt and generate secret
        totpActivationAttemptService.recordAttempt(user);
        String secretKey = generateSecretKey();

        // Generate setup information
        String qrCodeUri = generateQRCodeUri(user.getEmail(), secretKey);
        String manualEntryKey = formatSecretForManualEntry(secretKey);

        // Store secret in JWT activation token (encrypted for security)
        String encryptedSecret = encryptionService.encrypt(secretKey);
        String activationToken = twoFactorJwtService.generateActivationToken(user, encryptedSecret);

        log.info("TOTP 2FA activation initiated for user: {} (secret stored in token)", user.getUsername());

        return new TotpActivationInitiateResult(qrCodeUri, secretKey, manualEntryKey, activationToken);
    }

    @Override
    @Transactional
    public void verifyAndActivateTotpTwoFactor(TotpTwoFactorActivationBllRequest request) {
        log.info("Starting TOTP 2FA verification and activation process");

        // Validate activation token and extract claims
        Claims claims = twoFactorJwtService.validateActivationToken(request.activationToken());
        String userPublicId = claims.get("publicId", String.class);
        String encryptedSecret = claims.get("verificationCode", String.class); // We reuse this claim for the secret

        // Load user by publicId from token
        User user = userService.getUserByPublicId(userPublicId);
        log.debug("Processing TOTP 2FA activation for user: {}", user.getUsername());

        // Rate limiting check for verification attempts
        if (totpVerificationAttemptService.hasExceededAttempts(user)) {
            log.warn("User {} exceeded TOTP 2FA verification attempts", user.getUsername());
            throw new MaxAttemptsReachedException("Too many verification attempts. Please try again later.");
        }

        // Validate TOTP 2FA not already enabled
        if (twoFactorAuthRepository.existsByUserAndTypeAndEnabledTrue(user, TwoFactorType.TOTP)) {
            log.warn("TOTP 2FA already enabled for user: {}", user.getUsername());
            throw new TOTPTwoFactorAlreadyEnabledException("TOTP two-factor authentication is already enabled");
        }

        // Decrypt the secret and verify the TOTP code
        String secretKey = encryptionService.decrypt(encryptedSecret);
        if (!verifyTotpCode(secretKey, request.getTrimmedVerificationCode())) {
            log.warn("Invalid TOTP code provided during activation for user: {}", user.getUsername());
            totpVerificationAttemptService.recordAttempt(user);
            throw new InvalidVerificationCodeException("The verification code is incorrect", 400);
        }

        // Code verification successful - create and save TOTP config
        createAndSaveTotpTwoFactorEntity(user, secretKey);

        // Publish confirmation event and reset rate limiting
        Device device = deviceService.detectCurrentDevice();
        eventPublisher.publishEvent(new TwoFactorEnabledEvent(user, device, TwoFactorType.TOTP));
        totpActivationAttemptService.resetAttempts(user);
        totpVerificationAttemptService.resetAttempts(user);

        log.info("TOTP 2FA successfully activated for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void disable() {
        User user = userService.getAuthenticatedUser();
        log.info("Disabling TOTP 2FA for user: {}", user.getUsername());

        TwoFactorAuth totpAuth = twoFactorAuthRepository
                .findByUserAndTypeAndEnabledTrue(user, TwoFactorType.TOTP)
                .orElseThrow(() -> {
                    log.warn("Attempted to disable TOTP 2FA for user {} but it's not enabled", user.getUsername());
                    return new TOTPTwoFactorNotEnabledException("TOTP two-factor authentication is not enabled");
                });

        // Disable the configuration (don't delete for audit purposes)
        totpAuth.setEnabled(false);
        totpAuth.setIsPrimary(false);
        totpAuth.setDisabledAt(Instant.now());

        twoFactorAuthRepository.save(totpAuth);
        log.info("TOTP 2FA disabled successfully for user: {}", user.getUsername());

        // Publish activity log event
        Device device = deviceService.detectCurrentDevice();
        eventPublisher.publishEvent(new TwoFactorDisabledEvent(user, device, TwoFactorType.TOTP));
    }

    @Override
    public String generateCode(User user) {
        log.debug("Generating TOTP code for user: {}", user.getUsername());

        TwoFactorAuth totpAuth = twoFactorAuthRepository
                .findByUserAndTypeAndEnabledTrue(user, TwoFactorType.TOTP)
                .orElseThrow(() -> {
                    log.warn("Attempted to generate TOTP code for user {} but TOTP 2FA is not enabled", user.getUsername());
                    return new TOTPTwoFactorNotEnabledException("TOTP two-factor authentication is not enabled");
                });

        // Decrypt the secret key before using it
        String secretKey = encryptionService.decrypt(totpAuth.getSecret());

        // Generate code for current time
        long currentTime = Instant.now().getEpochSecond();
        return generateTOTPCode(secretKey, currentTime);
    }

    @Override
    public boolean verifyCode(User user, String providedCode) {
        log.debug("Verifying TOTP code for user: {}", user.getUsername());

        TwoFactorAuth totpAuth = twoFactorAuthRepository
                .findByUserAndTypeAndEnabledTrue(user, TwoFactorType.TOTP)
                .orElseThrow(() -> {
                    log.warn("Attempted to verify TOTP code for user {} but TOTP 2FA is not enabled", user.getUsername());
                    return new TOTPTwoFactorNotEnabledException("TOTP two-factor authentication is not enabled");
                });

        // Decrypt the secret key before using it
        String secretKey = encryptionService.decrypt(totpAuth.getSecret());

        // Check current time window and adjacent windows for tolerance
        long currentTime = Instant.now().getEpochSecond();

        // DEBUG LOGS
        log.info("=== TOTP VERIFICATION DEBUG ===");
        log.info("User: {}", user.getUsername());
        log.info("Provided code: '{}'", providedCode);
        log.info("Secret key: {}...", secretKey.substring(0, Math.min(8, secretKey.length())));
        log.info("Current timestamp: {}", currentTime);
        log.info("Time step: {} seconds", totpConfig.getTimeStepSeconds());
        log.info("Window tolerance: ±{} periods", totpConfig.getWindowTolerance());

        for (int i = -totpConfig.getWindowTolerance(); i <= totpConfig.getWindowTolerance(); i++) {
            long timeWindow = currentTime + (i * totpConfig.getTimeStepSeconds());
            String expectedCode = generateTOTPCode(secretKey, timeWindow);

            // DEBUG LOG pour chaque fenêtre de temps
            log.info("Time window {} ({}): expectedCode='{}', match={}",
                    i, timeWindow, expectedCode, expectedCode.equals(providedCode));

            // Use constant-time comparison to prevent timing attacks
            if (constantTimeEquals(providedCode, expectedCode)) {
                log.info("✅ TOTP code verified successfully for user: {} (time window: {})",
                        user.getUsername(), i);
                return true;
            }
        }

        log.warn("❌ TOTP code verification failed for user: {} - no matching codes found", user.getUsername());
        return false;
    }

    /**
     * Generate a cryptographically secure secret key for TOTP.
     */
    private String generateSecretKey() {
        byte[] secretBytes = new byte[totpConfig.getSecretLength()];
        secureRandom.nextBytes(secretBytes);
        return encodeBase32(secretBytes);
    }

    /**
     * Generate a TOTP code using RFC 6238 algorithm.
     */
    private String generateTOTPCode(String secretKey, long timeSeconds) {
        try {
            // Calculate time counter (RFC 6238)
            long timeCounter = timeSeconds / totpConfig.getTimeStepSeconds();

            // Convert counter to byte array (big-endian)
            byte[] timeBytes = new byte[8];
            for (int i = 7; i >= 0; i--) {
                timeBytes[i] = (byte) (timeCounter & 0xFF);
                timeCounter >>= 8;
            }

            // Decode Base32 secret
            byte[] secretBytes = decodeBase32(secretKey);

            // HMAC-SHA1
            Mac mac = Mac.getInstance(totpConfig.getHmacAlgorithm());
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretBytes, totpConfig.getHmacAlgorithm());
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(timeBytes);

            // Dynamic truncation (RFC 4226)
            int offset = hash[hash.length - 1] & 0x0F;
            int code = ((hash[offset] & 0x7F) << 24) |
                    ((hash[offset + 1] & 0xFF) << 16) |
                    ((hash[offset + 2] & 0xFF) << 8) |
                    (hash[offset + 3] & 0xFF);

            // Generate 6-digit code
            code = code % (int) Math.pow(10, totpConfig.getCodeDigits());

            return String.format("%0" + totpConfig.getCodeDigits() + "d", code);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating TOTP code: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate TOTP code", e);
        }
    }

    /**
     * Generate QR code URI for authenticator app setup.
     */
    private String generateQRCodeUri(String email, String secretKey) {
        String issuer = URLEncoder.encode(totpConfig.getIssuerName(), StandardCharsets.UTF_8);
        String account = URLEncoder.encode(email, StandardCharsets.UTF_8);

        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=%d&period=%d",
                issuer, account, secretKey, issuer, totpConfig.getCodeDigits(), totpConfig.getTimeStepSeconds()
        );
    }

    /**
     * Format secret key for manual entry (groups of 4 characters).
     */
    private String formatSecretForManualEntry(String secretKey) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < secretKey.length(); i += 4) {
            if (i > 0) formatted.append(" ");
            formatted.append(secretKey.substring(i, Math.min(i + 4, secretKey.length())));
        }
        return formatted.toString();
    }

    /**
     * Demote existing primary 2FA method to secondary.
     */
    private void demoteExistingPrimaryTwoFactor(User user) {
        twoFactorAuthRepository.findByUserAndIsPrimaryTrue(user)
                .ifPresent(existingPrimary -> {
                    existingPrimary.setIsPrimary(false);
                    twoFactorAuthRepository.save(existingPrimary);
                    log.debug("Demoted existing primary 2FA method {} for user: {}",
                            existingPrimary.getType(), user.getUsername());
                });
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * Encode bytes to Base32 string (RFC 4648).
     */
    private String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;

        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;

            while (bitsLeft >= 5) {
                result.append(BASE32_ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }

        if (bitsLeft > 0) {
            result.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }

        return result.toString();
    }

    /**
     * Decode Base32 string to bytes (RFC 4648).
     */
    private byte[] decodeBase32(String data) {
        data = data.toUpperCase().replaceAll("[^A-Z2-7]", "");

        byte[] result = new byte[data.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int resultIndex = 0;

        for (char c : data.toCharArray()) {
            int value = BASE32_ALPHABET.indexOf(c);
            if (value < 0) continue;

            buffer = (buffer << 5) | value;
            bitsLeft += 5;

            if (bitsLeft >= 8) {
                result[resultIndex++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }

        return result;
    }


    /**
     * Verify a TOTP code against a secret key (for activation flow).
     * Uses time window tolerance for slight clock differences.
     */
    private boolean verifyTotpCode(String secretKey, String providedCode) {
        long currentTime = Instant.now().getEpochSecond();

        for (int i = -totpConfig.getWindowTolerance(); i <= totpConfig.getWindowTolerance(); i++) {
            long timeWindow = currentTime + (i * totpConfig.getTimeStepSeconds());
            String expectedCode = generateTOTPCode(secretKey, timeWindow);

            if (constantTimeEquals(providedCode, expectedCode)) {
                log.debug("TOTP code verified successfully (time window: {})", i);
                return true;
            }
        }

        return false;
    }

    /**
     * Create and save TOTP 2FA entity for the user.
     */
    private void createAndSaveTotpTwoFactorEntity(User user, String secretKey) {
        // Check if a disabled TOTP 2FA configuration already exists
        Optional<TwoFactorAuth> existingTotpTwoFactor = twoFactorAuthRepository
                .findByUserAndType(user, TwoFactorType.TOTP);

        TwoFactorAuth totpAuth;
        if (existingTotpTwoFactor.isPresent()) {
            // Reactivate existing configuration with new secret
            totpAuth = existingTotpTwoFactor.get();
            totpAuth.setSecret(encryptionService.encrypt(secretKey));
            totpAuth.setEnabled(true);
            totpAuth.setEnabledAt(Instant.now());
            totpAuth.setDisabledAt(null);
            log.debug("Reactivated existing TOTP 2FA configuration for user: {}", user.getUsername());
        } else {
            // Create new TOTP 2FA configuration
            totpAuth = TwoFactorAuth.builder()
                    .user(user)
                    .type(TwoFactorType.TOTP)
                    .secret(encryptionService.encrypt(secretKey))
                    .enabled(true)
                    .isPrimary(true)
                    .enabledAt(Instant.now())
                    .build();
            log.debug("Created new TOTP 2FA configuration for user: {}", user.getUsername());
        }

        // Demote any existing primary 2FA method
        demoteExistingPrimaryTwoFactor(user);

        // Set this as primary and save
        totpAuth.setIsPrimary(true);
        twoFactorAuthRepository.save(totpAuth);
    }
}