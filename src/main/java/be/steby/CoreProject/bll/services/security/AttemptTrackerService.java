package be.steby.CoreProject.bll.services.security;

import be.steby.CoreProject.dl.entities.User;

public interface AttemptTrackerService {
    boolean hasExceededAttempts(User user);

    void recordAttempt(User user);

    void resetAttempts(User user);
}
