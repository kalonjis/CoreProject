package be.steby.CoreProject.bll.domains.auth.models;


import be.steby.CoreProject.dl.enums.TwoFactorType;

/**
 * Record representing the result of a two-factor authentication activation initiation.
 *
 * This immutable data structure contains:
 * - twoFactorType: The type of 2FA being activated (EMAIL, SMS, TOTP)
 * - twoFaActivationToken: JWT token containing verification code and user info
 *
 * Note: No success field is included as exceptions are thrown on failure.
 *
 * @param twoFactorType the type of two-factor authentication being activated
 * @param twoFaActivationToken the JWT token for the activation process
 */
public record TwoFactorActivationResult(
    TwoFactorType twoFactorType,
    String twoFaActivationToken
) {

    /**
     * Validates the activation result parameters.
     *
     * @throws IllegalArgumentException if any required parameter is null or invalid
     */
    public TwoFactorActivationResult {
        if (twoFactorType == null) {
            throw new IllegalArgumentException("Two factor type cannot be null");
        }
        if (twoFaActivationToken == null || twoFaActivationToken.isBlank()) {
            throw new IllegalArgumentException("Two factor activation token cannot be null or blank");
        }
    }
}