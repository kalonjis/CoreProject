package be.steby.CoreProject.bll.domains.device.services;

import be.steby.CoreProject.bll.common.services.tokens.AttemptTrackerService;
import be.steby.CoreProject.dl.entities.User;

public interface DeviceAwareAttemptTrackerService extends AttemptTrackerService {

    boolean hasExceededAttempts(User user, Long deviceId);

    void recordAttempt(User user, Long deviceId);

    void resetAttempts(User user, Long deviceId);

    int getAttemptCount(User user, Long deviceId);
}
