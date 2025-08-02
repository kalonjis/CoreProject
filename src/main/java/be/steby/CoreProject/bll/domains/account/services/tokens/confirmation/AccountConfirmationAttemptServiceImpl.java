package be.steby.CoreProject.bll.domains.account.services.tokens.confirmation;

import be.steby.CoreProject.bll.common.services.tokens.BaseAttemptTrackerServiceImpl;
import be.steby.CoreProject.dal.repositories.UserAttemptRepository;
import be.steby.CoreProject.dl.enums.AttemptType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class AccountConfirmationAttemptServiceImpl extends BaseAttemptTrackerServiceImpl {

    public AccountConfirmationAttemptServiceImpl(
            UserAttemptRepository userAttemptRepository,
            @Value("${security.account-confirmation.max-attempts}") int maxAttempts,
            @Value("${security.account-confirmation.lockout-minutes}") int lockoutMinutes) {
        super(userAttemptRepository, maxAttempts, lockoutMinutes, AttemptType.ACCOUNT_CONFIRMATION);
    }
}



