
// TwoFactorActivationInitiatedEvent.java
package be.steby.CoreProject.bll.domains.auth.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;

/**
 * Published when a two-factor authentication setup is initiated and a
 * verification code needs to be delivered to the user.
 *
 * <p>Replaces the former {@code TwoFactorInitiateActivationEvent} (email-only)
 * and {@code TwoFactorSmsActivationEvent} (SMS-only) into a single unified
 * event. The {@code type} field drives the delivery channel in the listener.</p>
 *
 * <p>Published by: {@code EmailTwoFactorServiceImpl.initiateActivation()},
 * {@code SmsTwoFactorServiceImpl.initiateActivation()}</p>
 *
 * @param user             the user initiating 2FA setup; never {@code null}
 * @param device           the device from which the setup was initiated; never {@code null}
 * @param type             the 2FA method being activated (EMAIL, SMS, TOTP, …)
 * @param verificationCode the plain 6-digit code to deliver; never {@code null}
 */
public record TwoFactorActivationInitiatedEvent(
        User user,
        Device device,
        TwoFactorType type,
        String verificationCode
) {}