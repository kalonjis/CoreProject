package be.steby.CoreProject.bll.domains.device.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

/**
 * Published when a user remotely disconnects a specific device.
 *
 * <p>At the time this event is fired, all refresh tokens associated
 * with the target device have already been revoked.</p>
 *
 * @param user               the authenticated user who triggered the disconnection; never null
 * @param actorDevice        the device from which the action was initiated; never null
 * @param disconnectedDevice the device that was disconnected; never null
 */
public record DeviceDisconnectedEvent(
        User user,
        Device actorDevice,
        Device disconnectedDevice
) {}