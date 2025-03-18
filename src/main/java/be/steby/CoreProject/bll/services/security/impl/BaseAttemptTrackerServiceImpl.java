package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.bll.services.security.AttemptTrackerService;
import be.steby.CoreProject.dal.repositories.UserAttemptRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.UserAttempt;
import be.steby.CoreProject.dl.enums.AttemptType;
import org.springframework.transaction.annotation.Transactional;
import lombok.Getter;


import java.time.Instant;

public abstract class BaseAttemptTrackerServiceImpl implements AttemptTrackerService {

    private final UserAttemptRepository userAttemptRepository;

    @Getter
    private final int maxAttempts;

    @Getter
    private final int lockoutMinutes;

    @Getter
    private final AttemptType attemptType;

    protected BaseAttemptTrackerServiceImpl(
            UserAttemptRepository userAttemptRepository,
            int maxAttempts,
            int lockoutMinutes,
            AttemptType attemptType) {
        if (maxAttempts <= 0 || lockoutMinutes <= 0) {
            throw new IllegalArgumentException("maxAttempts and lockoutMinutes must be positive");
        }
        this.userAttemptRepository = userAttemptRepository;
        this.maxAttempts = maxAttempts;
        this.lockoutMinutes = lockoutMinutes;
        this.attemptType = attemptType;
    }

    protected UserAttempt getOrCreateAttempt(User user) {
        return userAttemptRepository.findByUserAndAttemptType(user, attemptType)
                .orElse(new UserAttempt(user, attemptType));
    }

    protected int getAttempts(User user) {
        return userAttemptRepository.findByUserAndAttemptType(user, attemptType)
                .map(UserAttempt::getAttemptCount)
                .orElse(0);
    }

    @Transactional
    protected void incrementAttempts(User user) {
        UserAttempt attempt = getOrCreateAttempt(user);
        attempt.setAttemptCount(attempt.getAttemptCount() + 1);
        attempt.setLastAttemptTime(Instant.now());
        userAttemptRepository.save(attempt);
    }

    @Transactional
    protected void clearAttempts(User user) {
        userAttemptRepository.findByUserAndAttemptType(user, attemptType)
                .ifPresent(attempt -> {
                    attempt.setAttemptCount(0);
                    attempt.setLastAttemptTime(null);
                    userAttemptRepository.save(attempt);
                });
    }

    @Override
    public Instant getLastAttemptTime(User user) {
        return userAttemptRepository.findByUserAndAttemptType(user, attemptType)
                .map(UserAttempt::getLastAttemptTime)
                .orElse(null);
    }



    @Override
    @Transactional(readOnly = true)
    public int getAttemptCount(User user) {
        return getAttempts(user);
    }



    @Override
    @Transactional(readOnly = true)
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
    @Transactional
    public void recordAttempt(User user) {
        incrementAttempts(user);
    }

    @Override
    @Transactional
    public void resetAttempts(User user) {
        clearAttempts(user);
    }
}
