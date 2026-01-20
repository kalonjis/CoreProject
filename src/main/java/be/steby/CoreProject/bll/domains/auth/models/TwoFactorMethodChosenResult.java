package be.steby.CoreProject.bll.domains.auth.models;

import be.steby.CoreProject.dl.enums.TwoFactorType;

import java.util.List;

/**
 * Result object for two-factor method selection operation.
 *
 * Contains the JWT token and information about the chosen method
 * for the presentation layer to handle appropriately.
 *
 * Supports two outcomes:
 * - Success: Code was sent, token is available
 * - Delivery failed: Code could not be sent, alternatives are provided
 *
 * @param twoFactorToken JWT token for the verification phase (null if delivery failed)
 * @param chosenMethod the 2FA method that was selected
 * @param codeGenerated whether a verification code was generated and sent
 * @param maskedTarget where the code was sent (if applicable)
 * @param deliveryFailed true if the code could not be delivered
 * @param failureReason reason for delivery failure (null if success)
 * @param alternativeMethods available 2FA methods to fall back to (empty if success)
 *
 * @author Steby Team
 * @since 2.0.0
 */
public record TwoFactorMethodChosenResult(
        String twoFactorToken,
        TwoFactorType chosenMethod,
        boolean codeGenerated,
        String maskedTarget,
        boolean deliveryFailed,
        String failureReason,
        List<TwoFactorType> alternativeMethods
) {

    /**
     * Creates a successful result when the code was sent.
     *
     * @param twoFactorToken JWT token for verification phase
     * @param chosenMethod the 2FA method selected
     * @param codeGenerated whether a code was generated (false for TOTP/backup)
     * @param maskedTarget where the code was sent (e.g., "j***@mail.com")
     * @return successful result
     */
    public static TwoFactorMethodChosenResult success(
            String twoFactorToken,
            TwoFactorType chosenMethod,
            boolean codeGenerated,
            String maskedTarget) {
        return new TwoFactorMethodChosenResult(
                twoFactorToken,
                chosenMethod,
                codeGenerated,
                maskedTarget,
                false,
                null,
                List.of()
        );
    }

    /**
     * Creates a failure result when code delivery failed.
     *
     * @param chosenMethod the 2FA method that was attempted
     * @param failureReason human-readable reason for the failure
     * @param alternativeMethods available methods the user can fall back to
     * @return failure result with alternatives
     */
    public static TwoFactorMethodChosenResult deliveryFailed(
            TwoFactorType chosenMethod,
            String failureReason,
            List<TwoFactorType> alternativeMethods) {
        return new TwoFactorMethodChosenResult(
                null,
                chosenMethod,
                false,
                null,
                true,
                failureReason,
                alternativeMethods
        );
    }

    /**
     * Checks if this result represents a successful operation.
     *
     * @return true if delivery succeeded
     */
    public boolean isSuccess() {
        return !deliveryFailed;
    }
}