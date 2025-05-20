package be.steby.CoreProject.bll.domains.emailAddress.services.tokens;

import be.steby.CoreProject.bll.common.services.tokens.BaseAttemptTrackerServiceImpl;
import be.steby.CoreProject.dal.repositories.UserAttemptRepository;
import be.steby.CoreProject.dl.enums.AttemptType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class EmailConfirmationAttemptServiceImpl extends BaseAttemptTrackerServiceImpl {

    public EmailConfirmationAttemptServiceImpl(
            UserAttemptRepository userAttemptRepository,
            @Value("${security.email-confirmation.max-attempts}") int maxAttempts,
            @Value("${security.email-confirmation.lockout-minutes}") int lockoutMinutes) {
        super(userAttemptRepository, maxAttempts, lockoutMinutes, AttemptType.EMAIL_CONFIRMATION);
    }
}



