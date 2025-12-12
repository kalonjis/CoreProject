package be.steby.CoreProject.bll.domains.auth.services.twofactor.emailtwofactor;

import be.steby.CoreProject.bll.common.services.tokens.BaseAttemptTrackerServiceImpl;
import be.steby.CoreProject.dal.repositories.UserAttemptRepository;
import be.steby.CoreProject.dl.enums.AttemptType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service implementation for tracking and limiting email 2FA activation attempts.
 *
 * <p>This service extends {@link BaseAttemptTrackerServiceImpl} to provide rate limiting
 * functionality specifically for email-based two-factor authentication activation operations
 * (email sending). It prevents abuse of email services and protects against excessive
 * activation requests.</p>
 *
 * <h4>Rate Limiting Features:</h4>
 * <ul>
 *   <li>Limits the number of email 2FA activation attempts per user</li>
 *   <li>Implements lockout period after exceeding maximum attempts</li>
 *   <li>Tracks attempt counts and timestamps in database</li>
 *   <li>Provides methods to check, record, and reset attempt counts</li>
 * </ul>
 *
 * <h4>Security Benefits:</h4>
 * <ul>
 *   <li>Prevents email spam and service abuse</li>
 *   <li>Reduces costs associated with email services</li>
 *   <li>Provides audit trail of activation attempts</li>
 * </ul>
 *
 * <h4>Configuration:</h4>
 * <p>This service is configured via application properties:</p>
 * <ul>
 *   <li>{@code security.two-factor.email.activation.max-attempts} - Maximum attempts before lockout (default: 5)</li>
 *   <li>{@code security.two-factor.email.activation.lockout-minutes} - Duration of lockout period (default: 15)</li>
 * </ul>
 *
 * @see BaseAttemptTrackerServiceImpl
 * @see AttemptType#EMAIL_2FA_ACTIVATION
 * @see EmailTwoFactorServiceImpl
 * @see EmailTwoFactorVerificationAttemptService
 */
@Service
public class EmailTwoFactorActivationAttemptService extends BaseAttemptTrackerServiceImpl {

    /**
     * Constructs a new email 2FA activation attempt tracker service.
     *
     * <p>Configures the service to track EMAIL_2FA_ACTIVATION attempt type
     * with application-defined rate limiting parameters.</p>
     *
     * @param userAttemptRepository repository for persisting attempt records
     * @param maxAttempts maximum attempts before triggering lockout
     * @param lockoutMinutes duration of lockout period in minutes
     */
    public EmailTwoFactorActivationAttemptService(
            UserAttemptRepository userAttemptRepository,
            @Value("${security.two-factor.email.activation.max-attempts:5}") int maxAttempts,
            @Value("${security.two-factor.email.activation.lockout-minutes:15}") int lockoutMinutes) {
        super(userAttemptRepository, maxAttempts, lockoutMinutes, AttemptType.EMAIL_2FA_ACTIVATION);
    }
}