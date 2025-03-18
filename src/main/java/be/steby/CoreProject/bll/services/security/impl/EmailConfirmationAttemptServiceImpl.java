package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.dal.repositories.UserAttemptRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.AttemptType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;


@Service
public class EmailConfirmationAttemptServiceImpl extends BaseAttemptTrackerServiceImpl {

    public EmailConfirmationAttemptServiceImpl(
            UserAttemptRepository userAttemptRepository,
            @Value("${security.email-confirmation.max-attempts}") int maxAttempts,
            @Value("${security.email-confirmation.lockout-minutes}") int lockoutMinutes) {
        super(userAttemptRepository, maxAttempts, lockoutMinutes, AttemptType.EMAIL_CONFIRMATION);
    }
}



