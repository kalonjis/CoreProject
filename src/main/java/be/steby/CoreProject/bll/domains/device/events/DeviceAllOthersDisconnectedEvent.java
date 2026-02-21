package be.steby.CoreProject.bll.domains.device.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

/**
 * Published when a user disconnects all devices except their current one.
 *
 * <p>At the time this event is fired, all refresh tokens for the other
 * devices have already been revoked. The {@code disconnectedCount} reflects
 * the actual number of devices that were affected.</p>
 *
 * @param user              the authenticated user who triggered the action; never null
 * @param currentDevice     the device that was kept active; never null
 * @param disconnectedCount number of devices that were disconnected
 */
public record DeviceAllOthersDisconnectedEvent(
        User user,
        Device currentDevice,
        int disconnectedCount
) {}