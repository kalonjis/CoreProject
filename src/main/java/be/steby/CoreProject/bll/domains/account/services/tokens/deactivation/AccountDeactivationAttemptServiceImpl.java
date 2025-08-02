package be.steby.CoreProject.bll.domains.account.services.tokens.deactivation;

import be.steby.CoreProject.bll.common.services.tokens.BaseAttemptTrackerServiceImpl;
import be.steby.CoreProject.dal.repositories.UserAttemptRepository;
import be.steby.CoreProject.dl.enums.AttemptType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class AccountDeactivationAttemptServiceImpl extends BaseAttemptTrackerServiceImpl {

    public AccountDeactivationAttemptServiceImpl(
            UserAttemptRepository userAttemptRepository,
            @Value("${security.account-deactivation.max-attempts}") int maxAttempts,
            @Value("${security.account-deactivation.lockout-minutes}") int lockoutMinutes) {
        super(userAttemptRepository, maxAttempts, lockoutMinutes, AttemptType.ACCOUNT_DEACTIVATION);
    }
}



