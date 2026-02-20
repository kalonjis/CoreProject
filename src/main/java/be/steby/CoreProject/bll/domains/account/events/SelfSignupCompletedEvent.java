package be.steby.CoreProject.bll.domains.account.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

/**
 * Event published when a user completes self-signup.
 * Triggers activation email sending via event listener.
 */
public record SelfSignupCompletedEvent(
        User user,
        String activationToken,
        Device device
) {
}