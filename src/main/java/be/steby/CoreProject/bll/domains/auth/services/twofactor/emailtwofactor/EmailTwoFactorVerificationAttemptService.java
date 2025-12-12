package be.steby.CoreProject.bll.domains.auth.services.twofactor.emailtwofactor;

import be.steby.CoreProject.bll.common.services.tokens.BaseAttemptTrackerServiceImpl;
import be.steby.CoreProject.dal.repositories.UserAttemptRepository;
import be.steby.CoreProject.dl.enums.AttemptType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service implementation for tracking and limiting email 2FA verification attempts.
 *
 * <p>This service extends {@link BaseAttemptTrackerServiceImpl} to provide rate limiting
 * functionality specifically for email-based two-factor authentication verification operations
 * (code verification). It prevents brute force attacks on 6-digit verification codes.</p>
 *
 * <h4>Rate Limiting Features:</h4>
 * <ul>
 *   <li>Limits the number of email 2FA verification attempts per user</li>
 *   <li>Implements lockout period after exceeding maximum attempts</li>
 *   <li>Tracks attempt counts and timestamps in database</li>
 *   <li>Provides methods to check, record, and reset attempt counts</li>
 * </ul>
 *
 * <h4>Security Benefits:</h4>
 * <ul>
 *   <li>Protects against brute force attacks on 6-digit codes (1M combinations)</li>
 *   <li>Prevents automated code guessing attacks</li>
 *   <li>Provides audit trail of verification attempts</li>
 *   <li>Complements JWT token expiration for additional security</li>
 * </ul>
 *
 * <h4>Configuration:</h4>
 * <p>This service is configured via application properties:</p>
 * <ul>
 *   <li>{@code security.two-factor.email.verification.max-attempts} - Maximum attempts before lockout (default: 10)</li>
 *   <li>{@code security.two-factor.email.verification.lockout-minutes} - Duration of lockout period (default: 5)</li>
 * </ul>
 *
 * <h4>Usage Pattern:</h4>
 * <p>Used during the verification phase of email 2FA activation:</p>
 * <pre>
 * // Before verifying code
 * if (verificationAttemptService.hasExceededAttempts(user)) {
 *     throw new MaxAttemptsReachedException("Too many verification attempts");
 * }
 * verificationAttemptService.recordAttempt(user);
 *
 * // After successful verification and activation
 * verificationAttemptService.resetAttempts(user);
 * </pre>
 *
 * @see BaseAttemptTrackerServiceImpl
 * @see AttemptType#EMAIL_2FA_VERIFICATION
 * @see EmailTwoFactorServiceImpl
 * @see EmailTwoFactorActivationAttemptService
 */
@Service
public class EmailTwoFactorVerificationAttemptService extends BaseAttemptTrackerServiceImpl {

    /**
     * Constructs a new email 2FA verification attempt tracker service.
     *
     * <p>Configures the service to track EMAIL_2FA_VERIFICATION attempt type
     * with application-defined rate limiting parameters.</p>
     *
     * @param userAttemptRepository repository for persisting attempt records
     * @param maxAttempts maximum attempts before triggering lockout
     * @param lockoutMinutes duration of lockout period in minutes
     */
    public EmailTwoFactorVerificationAttemptService(
            UserAttemptRepository userAttemptRepository,
            @Value("${security.two-factor.email.verification.max-attempts:10}") int maxAttempts,
            @Value("${security.two-factor.email.verification.lockout-minutes:5}") int lockoutMinutes) {
        super(userAttemptRepository, maxAttempts, lockoutMinutes, AttemptType.EMAIL_2FA_VERIFICATION);
    }
}