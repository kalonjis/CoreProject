package be.steby.CoreProject.bll.domains.password.services.tokens.sms;

import be.steby.CoreProject.bll.common.services.tokens.BaseAttemptTrackerServiceImpl;
import be.steby.CoreProject.dal.repositories.UserAttemptRepository;
import be.steby.CoreProject.dl.enums.AttemptType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service implementation for tracking and limiting SMS password reset attempts.
 * 
 * <p>This service extends {@link BaseAttemptTrackerServiceImpl} to provide rate limiting
 * functionality specifically for SMS-based password reset operations. It prevents abuse
 * of SMS services and protects against brute force attacks on SMS verification codes.</p>
 * 
 * <h4>Rate Limiting Features:</h4>
 * <ul>
 *   <li>Limits the number of SMS password reset attempts per user</li>
 *   <li>Implements lockout period after exceeding maximum attempts</li>
 *   <li>Tracks attempt counts and timestamps in database</li>
 *   <li>Provides methods to check, record, and reset attempt counts</li>
 * </ul>
 * 
 * <h4>Security Benefits:</h4>
 * <ul>
 *   <li>Prevents SMS spam and service abuse</li>
 *   <li>Protects against brute force attacks on verification codes</li>
 *   <li>Reduces costs associated with SMS services</li>
 *   <li>Provides audit trail of reset attempts</li>
 * </ul>
 * 
 * <h4>Configuration:</h4>
 * <p>This service is configured via application properties:</p>
 * <ul>
 *   <li>{@code security.sms-password-reset.max-attempts} - Maximum attempts before lockout</li>
 *   <li>{@code security.sms-password-reset.lockout-minutes} - Duration of lockout period</li>
 * </ul>
 * 
 * <h4>Usage Pattern:</h4>
 * <p>Typically used by {@link SmsTokenServiceImpl} to enforce rate limits:</p>
 * <pre>
 * // Before creating SMS token
 * if (attemptService.hasExceededAttempts(user)) {
 *     throw new MaxAttemptsReachedException("Too many attempts");
 * }
 * attemptService.recordAttempt(user);
 * 
 * // After successful password reset
 * attemptService.resetAttempts(user);
 * </pre>
 * 
 * @see BaseAttemptTrackerServiceImpl
 * @see AttemptType#SMS_PASSWORD_RESET
 * @see SmsTokenServiceImpl
 */
@Service
public class SmsAttemptServiceImpl extends BaseAttemptTrackerServiceImpl {

    /**
     * Constructs a new SMS password reset attempt tracker service.
     * 
     * <p>Configuration is injected from application properties to allow
     * easy customization of rate limiting behavior without code changes.</p>
     * 
     * <h4>Default Recommended Configuration:</h4>
     * <ul>
     *   <li>Max attempts: 3-5 attempts per lockout period</li>
     *   <li>Lockout duration: 15-30 minutes</li>
     * </ul>
     * 
     * <p>These values balance security with user experience, preventing abuse
     * while not being overly restrictive for legitimate users.</p>
     *
     * @param userAttemptRepository Repository for persisting attempt data
     * @param maxAttempts Maximum number of attempts before lockout (from properties)
     * @param lockoutMinutes Duration of lockout period in minutes (from properties)
     */
    public SmsAttemptServiceImpl(
            UserAttemptRepository userAttemptRepository,
            @Value("${security.sms-password-reset.max-attempts:3}") int maxAttempts,
            @Value("${security.sms-password-reset.lockout-minutes:15}") int lockoutMinutes) {
        super(userAttemptRepository, maxAttempts, lockoutMinutes, AttemptType.SMS_PASSWORD_RESET);
    }
}