package be.steby.CoreProject.bll.domains.auth.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

/**
 * Event published when a known user fails to authenticate
 * Important for security monitoring - someone is trying to breach an existing account
 */
public record UserLoginFailedEvent(
        User user,           // Always non-null (existing user)
        Device device,       // May be null in some edge cases
        String failureReason // Password incorrect, account locked, etc.
) {}