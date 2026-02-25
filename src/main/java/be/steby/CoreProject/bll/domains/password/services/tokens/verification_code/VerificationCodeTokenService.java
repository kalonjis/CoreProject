package be.steby.CoreProject.bll.domains.password.services.tokens.verification_code;

import be.steby.CoreProject.bll.common.services.passwordgenerator.TemporaryPasswordGeneratorService;
import be.steby.CoreProject.bll.common.services.tokens.BaseTokenServiceImpl;
import be.steby.CoreProject.bll.common.services.tokens.SecureTokenService;
import be.steby.CoreProject.bll.common.exceptions.MaxAttemptsReachedException;
import be.steby.CoreProject.dal.repositories.tokens.VerificationCodeTokenRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.VerificationCodeToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Service implementation for managing SMS password reset tokens.
 * 
 * <p>This service extends {@link BaseTokenServiceImpl} and specifically handles operations
 * related to SMS-based password reset operations. It provides functionality for creating,
 * validating, and managing SMS verification codes with proper security measures.</p>
 * 
 * <h4>Key Features:</h4>
 * <ul>
 *   <li>SMS verification code generation and validation</li>
 *   <li>Rate limiting to prevent SMS spam and abuse</li>
 *   <li>Secure code storage using cryptographic hashing</li>
 *   <li>Automatic cleanup of expired and used tokens</li>
 *   <li>Comprehensive security and audit logging</li>
 * </ul>
 * 
 * <h4>Security Measures:</h4>
 * <ul>
 *   <li>Verification codes are hashed before storage (never stored in plain text)</li>
 *   <li>Rate limiting prevents brute force attacks and SMS abuse</li>
 *   <li>Tokens automatically expire after configured time period</li>
 *   <li>One active token per user to prevent confusion</li>
 * </ul>
 * 
 * <h4>Rate Limiting:</h4>
 * <p>Delegates rate limiting to dedicated {@link VerificationCodeAttemptServiceImpl} for proper
 * separation of concerns and consistency with other token services in the application.</p>
 * 
 * @see BaseTokenServiceImpl
 * @see VerificationCodeToken
 * @see VerificationCodeAttemptServiceImpl
 */
@Service
@Slf4j
public class VerificationCodeTokenService extends BaseTokenServiceImpl<VerificationCodeToken> {

    @Value("${security.sms-password-reset.token.expiration:600000}")
    private Long smsTokenDurationMs; // Default: 10 minutes

    private final VerificationCodeTokenRepository verificationCodeTokenRepository;
    private final VerificationCodeAttemptServiceImpl verificationCodeAttemptService;
    private final TemporaryPasswordGeneratorService passwordGeneratorService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs a new {@link VerificationCodeTokenService} using the provided dependencies.
     *
     * @param verificationCodeTokenRepository The repository specifically for SMS password reset tokens
     * @param verificationCodeAttemptService Service for tracking and limiting SMS-specific attempts
     * @param secureTokenService Service for secure token generation
     * @param passwordGeneratorService Service for generating verification codes
     * @param passwordEncoder Encoder for hashing verification codes
     */
    public VerificationCodeTokenService(
            @Qualifier("verificationCodeTokenRepository") VerificationCodeTokenRepository verificationCodeTokenRepository,
            VerificationCodeAttemptServiceImpl verificationCodeAttemptService,
            SecureTokenService secureTokenService,
            TemporaryPasswordGeneratorService passwordGeneratorService,
            PasswordEncoder passwordEncoder) {
        super(verificationCodeTokenRepository, VerificationCodeToken.class, secureTokenService);
        this.verificationCodeTokenRepository = verificationCodeTokenRepository;
        this.verificationCodeAttemptService = verificationCodeAttemptService;
        this.passwordGeneratorService = passwordGeneratorService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a new SMS password reset token with verification code for a user.
     * 
     * <p>This method implements comprehensive rate limiting and security measures:</p>
     * <ol>
     *   <li>Checks SMS-specific rate limits via dedicated attempt service</li>
     *   <li>Generates secure 6-digit verification code</li>
     *   <li>Revokes any existing SMS tokens for the user</li>
     *   <li>Creates and saves new token with hashed code</li>
     * </ol>
     * 
     * <p><strong>Rate Limiting:</strong> Uses dedicated SMS attempt service for proper separation of concerns.</p>
     * <p><strong>Security:</strong> Verification codes are immediately hashed and never stored in plain text.</p>
     *
     * @param user The user requesting SMS password reset
     * @return SmsCodeGenerationResult containing the token and plain verification code for SMS sending
     * @throws MaxAttemptsReachedException if rate limits are exceeded
     */
    @Transactional
    public CodeGenerationResult createSmsPasswordResetToken(User user) {
        log.debug("Creating SMS password reset token for user: {}", user.getUsername());

        // Check SMS-specific rate limiting
        if (verificationCodeAttemptService.hasExceededAttempts(user)) {
            log.warn("User {} exceeded SMS password reset attempts", user.getUsername());
            throw new MaxAttemptsReachedException("Too many SMS password reset attempts. Please try again later.");
        }

        // Generate secure 6-digit verification code
        String verificationCode = passwordGeneratorService.generateNumericCode(6);
        log.debug("Generated verification code for user: {}", user.getUsername());

        // Hash the verification code for secure storage
        String codeHash = passwordEncoder.encode(verificationCode);

        // Revoke any existing SMS tokens for this user (one active token policy)
        verificationCodeTokenRepository.revokeAllUserTokens(user);

        // Create new token with hashed code
        VerificationCodeToken token = super.createToken(user, TokenType.SMS_PASSWORD_RESET, smsTokenDurationMs, false);
        token.setVerificationCodeHash(codeHash);
        verificationCodeTokenRepository.save(token);

        // Record attempt for rate limiting
        verificationCodeAttemptService.recordAttempt(user);

        log.info("SMS password reset token created successfully for user: {} (token: {})", 
                user.getUsername(), token.getToken());

        return new CodeGenerationResult(token, verificationCode);
    }

    /**
     * Validates a user-provided verification code against stored hash.
     * 
     * <p>This method performs comprehensive validation:</p>
     * <ol>
     *   <li>Looks up token by user and validates it's active and not expired</li>
     *   <li>Compares provided code with stored hash</li>
     *   <li>Revokes token on successful validation (one-time use)</li>
     *   <li>Resets attempt counters on success</li>
     * </ol>
     * 
     * <p><strong>Security:</strong> Uses secure password encoder for code comparison.</p>
     * <p><strong>One-time use:</strong> Tokens are automatically revoked after successful validation.</p>
     *
     * @param user The user attempting validation
     * @param providedCode The 6-digit code provided by the user
     * @return true if validation successful, false otherwise
     */
    @Transactional
    public boolean validateVerificationCode(User user, String providedCode) {
        log.debug("Validating SMS verification code for user: {}", user.getUsername());

        // Find active SMS token for user
        Optional<VerificationCodeToken> tokenOpt = verificationCodeTokenRepository.findByUserAndRevokedFalse(user);
        if (tokenOpt.isEmpty()) {
            log.debug("No active SMS token found for user: {}", user.getUsername());
            return false;
        }

        VerificationCodeToken token = tokenOpt.get();

        // Verify token is still valid (not expired)
        if (token.isExpired()) {
            log.debug("SMS token expired for user: {}", user.getUsername());
            revokeToken(token);
            return false;
        }

        // Validate the verification code
        boolean isValid = passwordEncoder.matches(providedCode, token.getVerificationCodeHash());
        
        if (isValid) {
            // Success: revoke token (one-time use) and reset attempt counters
            revokeToken(token);
            verificationCodeAttemptService.resetAttempts(user);
            log.info("SMS verification code validated successfully for user: {}", user.getUsername());
        } else {
            log.debug("Invalid SMS verification code provided for user: {}", user.getUsername());
        }

        return isValid;
    }

    /**
     * Finds active SMS password reset token for a user by token reference.
     * 
     * <p>Used during validation flow when JWT cookie contains token reference.
     * Validates token existence, ownership, and expiry status.</p>
     *
     * @param tokenReference The token reference from JWT cookie
     * @param user The user who should own this token (security check)
     * @return Optional containing the token if found and valid
     */
    @Transactional(readOnly = true)
    public Optional<VerificationCodeToken> findValidTokenByReference(String tokenReference, User user) {
        Optional<VerificationCodeToken> tokenOpt = verificationCodeTokenRepository.findValidByToken(tokenReference, Instant.now());
        
        if (tokenOpt.isPresent()) {
            VerificationCodeToken token = tokenOpt.get();
            // Security check: ensure token belongs to the requesting user
            if (!token.getUser().getId().equals(user.getId())) {
                log.warn("Token ownership mismatch: token {} requested by user {} but belongs to user {}", 
                        tokenReference, user.getUsername(), token.getUser().getUsername());
                return Optional.empty();
            }
        }
        
        return tokenOpt;
    }

    /**
     * Finds valid SMS token by its token reference (UUID).
     *
     * @param tokenReference the UUID token reference
     * @return Optional containing the token if found and valid
     */
    @Transactional(readOnly = true)
    public Optional<VerificationCodeToken> findValidTokenByReference(String tokenReference) {
        return verificationCodeTokenRepository.findValidByToken(tokenReference, Instant.now());
    }

    /**
     * Gets the expiration time in seconds for SMS tokens.
     * Used for JWT cookie configuration and client-side timers.
     *
     * @return token duration in seconds
     */
    public int getSmsTokenDurationInSeconds() {
        long durationInSeconds = smsTokenDurationMs / 1000;
        if (durationInSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("SMS token duration too large");
        }
        return (int) durationInSeconds;
    }

    // ========== SCHEDULED CLEANUP ==========

    /**
     * Scheduled task to clean up expired and revoked SMS tokens.
     * 
     * <p>Runs every hour to maintain database performance and remove
     * tokens that are no longer needed. This prevents database bloat
     * and ensures optimal query performance.</p>
     * 
     * <p><strong>Cleanup Policy:</strong> Removes tokens that are either expired OR revoked.</p>
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void cleanupExpiredTokens() {
        log.debug("Starting cleanup of expired and revoked SMS password reset tokens");
        
        try {
            verificationCodeTokenRepository.deleteExpiredTokens(Instant.now());
            log.debug("SMS password reset token cleanup completed successfully");
        } catch (Exception e) {
            log.error("Failed to cleanup expired SMS password reset tokens", e);
        }
    }

    // ========== RESULT CLASSES ==========

    /**
     * Result class for verification code generation containing both the token and plain verification code.
     * The plain code is needed for SMS sending, while the token contains the hashed version.
     */
    public record CodeGenerationResult(
            VerificationCodeToken token,
            String plainVerificationCode
    ) {}
}