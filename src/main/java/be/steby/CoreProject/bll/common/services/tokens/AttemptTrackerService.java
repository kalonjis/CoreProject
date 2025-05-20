package be.steby.CoreProject.bll.common.services.tokens;

import be.steby.CoreProject.dl.entities.User;

import java.time.Instant;

public interface AttemptTrackerService {

    boolean hasExceededAttempts(User user);

    void recordAttempt(User user);

    void resetAttempts(User user);

    int getAttemptCount(User user);

    Instant getLastAttemptTime(User user);
}
