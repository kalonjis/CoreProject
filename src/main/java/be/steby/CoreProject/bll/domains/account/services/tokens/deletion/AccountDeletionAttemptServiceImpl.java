package be.steby.CoreProject.bll.domains.account.services.tokens.deletion;

import be.steby.CoreProject.bll.common.services.tokens.BaseAttemptTrackerServiceImpl;
import be.steby.CoreProject.dal.repositories.UserAttemptRepository;
import be.steby.CoreProject.dl.enums.AttemptType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Rate-limiting service for GDPR account deletion requests.
 *
 * <p>Extends {@link BaseAttemptTrackerServiceImpl} to track and enforce
 * attempt limits on deletion requests per user, independently of
 * deactivation or other token flows.
 *
 * <p>Configuration properties (defined in {@code application.yml} or equivalent):
 * <ul>
 *   <li>{@code security.account-deletion.max-attempts} — maximum allowed attempts before lockout</li>
 *   <li>{@code security.account-deletion.lockout-minutes} — lockout duration in minutes</li>
 * </ul>
 *
 * <p>Stricter limits than deactivation are recommended given the irreversible
 * nature of the operation.
 */
@Service
public class AccountDeletionAttemptServiceImpl extends BaseAttemptTrackerServiceImpl {

    public AccountDeletionAttemptServiceImpl(
            UserAttemptRepository userAttemptRepository,
            @Value("${security.account-deletion.max-attempts}") int maxAttempts,
            @Value("${security.account-deletion.lockout-minutes}") int lockoutMinutes) {
        super(userAttemptRepository, maxAttempts, lockoutMinutes, AttemptType.ACCOUNT_DELETION);
    }
}