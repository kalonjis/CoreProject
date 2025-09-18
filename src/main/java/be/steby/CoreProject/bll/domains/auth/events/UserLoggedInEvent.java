package be.steby.CoreProject.bll.domains.auth.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

/**
 * Event published when a user successfully logs in
 * Only successful logins trigger this event
 */
public record UserLoggedInEvent(
        User user,    // Always non-null (guaranteed by publisher)
        Device device // Always non-null (guaranteed by publisher)
) {}