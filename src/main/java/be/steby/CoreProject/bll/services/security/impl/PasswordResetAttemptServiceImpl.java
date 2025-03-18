package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.dal.repositories.UserAttemptRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.AttemptType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PasswordResetAttemptServiceImpl extends BaseAttemptTrackerServiceImpl {

    public PasswordResetAttemptServiceImpl(
            UserAttemptRepository userAttemptRepository,
            @Value("${security.password-reset.max-attempts}") int maxAttempts,
            @Value("${security.password-reset.lockout-minutes}") int lockoutMinutes) {
        super(userAttemptRepository, maxAttempts, lockoutMinutes, AttemptType.PASSWORD_RESSET);
    }


}