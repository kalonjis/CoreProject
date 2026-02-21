package be.steby.CoreProject.bll.domains.device.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

/**
 * Published when a user manually requests a new device confirmation link.
 *
 * <p>This event is fired after the confirmation email has been sent.
 * It is distinct from {@link DeviceSecurityEvent} (which handles
 * automated security notifications) and is used solely for audit purposes.</p>
 *
 * @param user   the authenticated user who requested the link; never null
 * @param device the device awaiting confirmation; never null
 */
public record DeviceConfirmationLinkRequestedEvent(
        User user,
        Device device
) {}