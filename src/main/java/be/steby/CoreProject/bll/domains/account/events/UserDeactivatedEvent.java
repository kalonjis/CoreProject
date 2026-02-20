package be.steby.CoreProject.bll.domains.account.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

/**
 * Event published when a user account is deactivated (self or admin).
 * Triggers automatic logout to ensure security.
 */
public record UserDeactivatedEvent(
        User user,
        Device device
) {
}