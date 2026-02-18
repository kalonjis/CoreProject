package be.steby.CoreProject.bll.domains.auth.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;

/**
 * Published when a user disables a two-factor authentication method.
 *
 * <p>Carries the minimum context needed for activity logging: who performed
 * the action, from which device, and which 2FA type was deactivated.</p>
 *
 * @param user          the user who disabled the 2FA method; never {@code null}
 * @param device        the device from which the action was performed; never {@code null}
 * @param twoFactorType the 2FA method that was disabled (EMAIL, SMS, TOTP, …)
 */
public record TwoFactorDisabledEvent(
        User user,
        Device device,
        TwoFactorType twoFactorType
) {}