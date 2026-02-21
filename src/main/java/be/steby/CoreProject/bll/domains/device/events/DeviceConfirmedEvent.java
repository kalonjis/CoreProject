package be.steby.CoreProject.bll.domains.device.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

/**
 * Published when a user confirms a device via the email confirmation link.
 *
 * <p>At the time this event is fired, the device trust level has already
 * been set to {@code TRUSTED} and the confirmation token has been revoked.</p>
 *
 * @param user        the owner of the confirmed device; never null
 * @param actorDevice the device from which the confirmation link was clicked; never null
 * @param device      the device that was confirmed; never null
 */
public record DeviceConfirmedEvent(
        User user,
        Device actorDevice,
        Device device
) {}