package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.bll.services.security.AttemptTrackerService;
import be.steby.CoreProject.dl.entities.User;
import lombok.Getter;

import java.time.Instant;


public abstract class  BaseAttemptTrackerServiceImpl implements AttemptTrackerService {
    @Getter
    private final int maxAttempts;
    @Getter
    private final int lockoutMinutes;

    protected BaseAttemptTrackerServiceImpl(int maxAttempts, int lockoutMinutes) {
        if (maxAttempts <= 0 || lockoutMinutes <= 0) {
            throw new IllegalArgumentException("maxAttempts and lockoutMinutes must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.lockoutMinutes = lockoutMinutes;
    }

    protected abstract int getAttempts(User user);
    protected abstract void incrementAttempts(User user);
    protected abstract void clearAttempts(User user);
    protected abstract Instant getLastAttemptTime(User user);
    protected abstract void setLastAttemptTime(User user, Instant time);

    @Override
    public boolean hasExceededAttempts(User user) {
        int attempts = getAttempts(user);
        if (attempts >= maxAttempts) {
            Instant lastAttempt = getLastAttemptTime(user);
            if (lastAttempt != null &&
                    Instant.now().isAfter(lastAttempt.plusSeconds(lockoutMinutes * 60))) {
                resetAttempts(user);
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void recordAttempt(User user) {
        incrementAttempts(user);
        setLastAttemptTime(user, Instant.now());
    }

    @Override
    public void resetAttempts(User user) {
        clearAttempts(user);
        setLastAttemptTime(user, null);
    }
}
