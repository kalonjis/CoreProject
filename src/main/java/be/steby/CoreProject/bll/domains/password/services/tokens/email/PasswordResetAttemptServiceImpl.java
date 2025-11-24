package be.steby.CoreProject.bll.domains.password.services.tokens.email;

import be.steby.CoreProject.bll.common.services.tokens.BaseAttemptTrackerServiceImpl;
import be.steby.CoreProject.dal.repositories.UserAttemptRepository;
import be.steby.CoreProject.dl.enums.AttemptType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetAttemptServiceImpl extends BaseAttemptTrackerServiceImpl {

    public PasswordResetAttemptServiceImpl(
            UserAttemptRepository userAttemptRepository,
            @Value("${security.password-reset.max-attempts}") int maxAttempts,
            @Value("${security.password-reset.lockout-minutes}") int lockoutMinutes) {
        super(userAttemptRepository, maxAttempts, lockoutMinutes, AttemptType.EMAIL_PASSWORD_RESET);
    }


}