package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AccountConfirmationAttemptServiceImpl extends BaseAttemptTrackerServiceImpl {
    private final UserService userService;

    public AccountConfirmationAttemptServiceImpl(
            @Value("${security.account-confirmation.max-attempts:5}") int maxAttempts,
            @Value("${security.account-confirmation.lockout-minutes:60}") int lockoutMinutes,
            UserService userService) {
        super(maxAttempts, lockoutMinutes);
        this.userService = userService;
    }

    @Override
    protected int getAttempts(User user) {
        return user.getAccountConfirmationAttempts();
    }

    @Override
    protected void incrementAttempts(User user) {
        user.setAccountConfirmationAttempts(user.getAccountConfirmationAttempts() + 1);
        userService.saveUser(user);
    }

    @Override
    protected void clearAttempts(User user) {
        user.setAccountConfirmationAttempts(0);
        userService.saveUser(user);
    }

    @Override
    protected Instant getLastAttemptTime(User user) {
        return user.getAccountConfirmationLastAttemptTime();
    }

    @Override
    protected void setLastAttemptTime(User user, Instant time) {
        user.setAccountConfirmationLastAttemptTime(time);
        userService.saveUser(user);
    }
}



