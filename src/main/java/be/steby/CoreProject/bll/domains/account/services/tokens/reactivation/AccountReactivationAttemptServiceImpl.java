package be.steby.CoreProject.bll.domains.account.services.tokens.reactivation;

import be.steby.CoreProject.bll.common.services.tokens.BaseAttemptTrackerServiceImpl;
import be.steby.CoreProject.dal.repositories.UserAttemptRepository;
import be.steby.CoreProject.dl.enums.AttemptType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class AccountReactivationAttemptServiceImpl extends BaseAttemptTrackerServiceImpl {

    public AccountReactivationAttemptServiceImpl(
            UserAttemptRepository userAttemptRepository,
            @Value("${security.account-reactivation.max-attempts}") int maxAttempts,
            @Value("${security.account-reactivation.lockout-minutes}") int lockoutMinutes) {
        super(userAttemptRepository, maxAttempts, lockoutMinutes, AttemptType.ACCOUNT_REACTIVATION);
    }
}



