package be.steby.CoreProject.bll.domains.device.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

/**
 * Published when a user rejects a device via the email confirmation link.
 *
 * <p>At the time this event is fired, the device has already been blacklisted,
 * its trust level set to {@code UNTRUSTED}, and all associated refresh tokens
 * have been revoked.</p>
 *
 * @param user        the owner of the rejected device; never null
 * @param actorDevice the device from which the rejection link was clicked; never null
 * @param device      the device that was rejected and blacklisted; never null
 */
public record DeviceRejectedEvent(
        User user,
        Device actorDevice,
        Device device
) {}