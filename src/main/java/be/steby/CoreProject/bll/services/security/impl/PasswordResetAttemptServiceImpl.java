package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PasswordResetAttemptServiceImpl extends BaseAttemptTrackerServiceImpl {
    private final UserService userService;

    public PasswordResetAttemptServiceImpl(
            @Value("${security.password-reset.max-attempts:3}") int maxAttempts,
            @Value("${security.password-reset.lockout-minutes:30}") int lockoutMinutes,
            UserService userService) {
        super(maxAttempts, lockoutMinutes);
        this.userService = userService;
    }

    @Override
    protected int getAttempts(User user) {
        return user.getResetPasswordAttempts();
    }

    @Override
    protected void incrementAttempts(User user) {
        user.setResetPasswordAttempts(user.getResetPasswordAttempts() + 1);
        userService.saveUser(user);
    }

    @Override
    protected void clearAttempts(User user) {
        user.setResetPasswordAttempts(0);
        userService.saveUser(user);
    }

    @Override
    protected Instant getLastAttemptTime(User user) {
        return user.getResetPasswordLastAttemptTime();
    }

    @Override
    protected void setLastAttemptTime(User user, Instant time) {
        user.setResetPasswordLastAttemptTime(time);
        userService.saveUser(user);
    }
}
