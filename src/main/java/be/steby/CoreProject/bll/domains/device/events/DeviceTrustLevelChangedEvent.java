package be.steby.CoreProject.bll.domains.device.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;


/**
 * Published when the trust level of a device is changed by the user.
 *
 * <p>Fired after the update has been persisted to the database.</p>
 *
 * @param newTrustLevel the trust level the device was changed to
 * @param oldLevel      the trust level the device had before the change
 * @param user          the authenticated user who triggered the change; never null
 * @param actorDevice   the device from which the action was initiated; never null
 * @param targetDevice        the device whose trust level was changed; never null
 */
public record DeviceTrustLevelChangedEvent(
        DeviceTrustLevel newTrustLevel,
        String oldLevel,
        User user,
        Device actorDevice,
        Device targetDevice
) {}