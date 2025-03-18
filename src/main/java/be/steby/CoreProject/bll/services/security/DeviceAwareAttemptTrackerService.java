package be.steby.CoreProject.bll.services.security;

import be.steby.CoreProject.dl.entities.User;

public interface DeviceAwareAttemptTrackerService extends AttemptTrackerService {

    boolean hasExceededAttempts(User user, Long deviceId);

    void recordAttempt(User user, Long deviceId);

    void resetAttempts(User user, Long deviceId);

    int getAttemptCount(User user, Long deviceId);
}
