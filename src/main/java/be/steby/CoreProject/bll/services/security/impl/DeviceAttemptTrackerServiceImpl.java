package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.bll.services.security.DeviceAwareAttemptTrackerService;
import be.steby.CoreProject.dal.repositories.UserAttemptRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.UserAttempt;
import be.steby.CoreProject.dl.enums.AttemptType;
import lombok.Getter;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public abstract class DeviceAttemptTrackerServiceImpl implements DeviceAwareAttemptTrackerService {

    private final UserAttemptRepository userAttemptRepository;

    @Getter
    private final int maxAttempts;

    @Getter
    private final int lockoutMinutes;

    @Getter
    private final AttemptType attemptType;

    protected DeviceAttemptTrackerServiceImpl(
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

    protected UserAttempt getOrCreateAttempt(User user, Long deviceId) {
        return userAttemptRepository.findByUserAndAttemptTypeAndDeviceId(user, attemptType, deviceId)
                .orElse(new UserAttempt(user, attemptType, deviceId));
    }

    protected int getAttempts(User user, Long deviceId) {
        return userAttemptRepository.findByUserAndAttemptTypeAndDeviceId(user, attemptType, deviceId)
                .map(UserAttempt::getAttemptCount)
                .orElse(0);
    }

    @Transactional
    protected void incrementAttempts(User user, Long deviceId) {
        UserAttempt attempt = getOrCreateAttempt(user, deviceId);
        attempt.setAttemptCount(attempt.getAttemptCount() + 1);
        attempt.setLastAttemptTime(Instant.now());
        userAttemptRepository.save(attempt);
    }

    @Transactional
    protected void clearAttempts(User user, Long deviceId) {
        userAttemptRepository.findByUserAndAttemptTypeAndDeviceId(user, attemptType, deviceId)
                .ifPresent(attempt -> {
                    attempt.setAttemptCount(0);
                    attempt.setLastAttemptTime(null);
                    userAttemptRepository.save(attempt);
                });
    }

    protected Instant getLastAttemptTime(User user, Long deviceId) {
        return userAttemptRepository.findByUserAndAttemptTypeAndDeviceId(user, attemptType, deviceId)
                .map(UserAttempt::getLastAttemptTime)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasExceededAttempts(User user, Long deviceId) {
        int attempts = getAttempts(user, deviceId);
        if (attempts >= maxAttempts) {
            Instant lastAttempt = getLastAttemptTime(user, deviceId);
            if (lastAttempt != null &&
                    Instant.now().isAfter(lastAttempt.plusSeconds(lockoutMinutes * 60))) {
                resetAttempts(user, deviceId);
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void recordAttempt(User user, Long deviceId) {
        incrementAttempts(user, deviceId);
    }

    @Override
    @Transactional
    public void resetAttempts(User user, Long deviceId) {
        clearAttempts(user, deviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getAttemptCount(User user, Long deviceId) {
        return getAttempts(user, deviceId);
    }

    // Implémentations pour la compatibilité avec AttemptTrackerService

    @Override
    public boolean hasExceededAttempts(User user) {
        throw new UnsupportedOperationException("Device ID is required for device-aware attempt tracking");
    }

    @Override
    public void recordAttempt(User user) {
        throw new UnsupportedOperationException("Device ID is required for device-aware attempt tracking");
    }

    @Override
    public void resetAttempts(User user) {
        throw new UnsupportedOperationException("Device ID is required for device-aware attempt tracking");
    }

    @Override
    public int getAttemptCount(User user) {
        throw new UnsupportedOperationException("Device ID is required for device-aware attempt tracking");
    }

    @Override
    public Instant getLastAttemptTime(User user) {
        throw new UnsupportedOperationException("Device ID is required for device-aware attempt tracking");
    }
}