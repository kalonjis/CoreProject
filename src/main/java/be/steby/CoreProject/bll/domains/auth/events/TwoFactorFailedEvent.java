package be.steby.CoreProject.bll.domains.auth.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;

/**
 * Event published when a user fails a two-factor authentication attempt.
 *
 * <p>This event is raised during the 2FA verification step, after the user
 * has already passed primary authentication (username + password). It covers
 * all 2FA methods: TOTP, EMAIL, SMS, BACKUP_CODES.</p>
 *
 * <p>Important for security monitoring — repeated failures may indicate
 * an attacker who has obtained the user's password and is attempting to
 * bypass 2FA.</p>
 *
 * <p>Published by: {@code AuthServiceImpl.verifyTwoFactor()}</p>
 *
 * @param user          The user who failed 2FA. Always non-null (user already
 *                      identified at this stage of the auth flow).
 * @param device        The device from which the attempt was made. May be null
 *                      in edge cases where device resolution failed.
 * @param twoFactorType The 2FA method that was attempted (TOTP, EMAIL, SMS,
 *                      BACKUP_CODES).
 * @param failureReason Human-readable reason for the failure, e.g.
 *                      "Invalid code", "Expired code", "Max attempts reached".
 */
public record TwoFactorFailedEvent(
        User user,
        Device device,
        TwoFactorType twoFactorType,
        String failureReason
) {}