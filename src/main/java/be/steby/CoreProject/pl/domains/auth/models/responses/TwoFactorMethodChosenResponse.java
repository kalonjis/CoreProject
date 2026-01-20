package be.steby.CoreProject.pl.domains.auth.models.responses;

import be.steby.CoreProject.bll.domains.auth.models.TwoFactorMethodChosenResult;
import be.steby.CoreProject.dl.enums.TwoFactorType;

import java.util.List;

/**
 * Response for 2FA method selection endpoint.
 *
 * Supports two outcomes:
 * - Success: Method selected, code sent (or ready for TOTP/backup)
 * - Delivery failed: Code could not be sent, alternatives provided
 *
 * @param message human-readable message
 * @param status operation status (2FA_METHOD_CHOSEN or 2FA_DELIVERY_FAILED)
 * @param chosenMethod the method that was selected
 * @param codeSent whether a verification code was sent
 * @param maskedTarget where the code was sent (e.g., "j***@mail.com")
 * @param deliveryFailed true if code delivery failed
 * @param alternativeMethods available fallback methods (only if deliveryFailed)
 *
 * @author Steby Team
 * @since 2.0.0
 */
public record TwoFactorMethodChosenResponse(
        String message,
        String status,
        TwoFactorType chosenMethod,
        boolean codeSent,
        String maskedTarget,
        boolean deliveryFailed,
        List<TwoFactorType> alternativeMethods
) {

    /**
     * Creates a success response from BLL result.
     *
     * @param result the successful BLL result
     * @return success response
     */
    public static TwoFactorMethodChosenResponse success(TwoFactorMethodChosenResult result) {
        String message = switch (result.chosenMethod()) {
            case EMAIL -> "Verification code sent to your email.";
            case SMS -> "Verification code sent to your phone.";
            case TOTP -> "Enter the code from your authenticator app.";
            case BACKUP_CODES -> "Enter one of your saved backup codes.";
            case WEBAUTHN -> "Use your security key to authenticate.";
        };

        return new TwoFactorMethodChosenResponse(
                message,
                "2FA_METHOD_CHOSEN",
                result.chosenMethod(),
                result.codeGenerated(),
                result.maskedTarget(),
                false,
                List.of()
        );
    }

    /**
     * Creates a delivery failure response from BLL result.
     *
     * @param result the failed BLL result
     * @return failure response with alternatives
     */
    public static TwoFactorMethodChosenResponse deliveryFailed(TwoFactorMethodChosenResult result) {
        return new TwoFactorMethodChosenResponse(
                result.failureReason() != null 
                        ? result.failureReason() 
                        : "Unable to send verification code. Please try another method.",
                "2FA_DELIVERY_FAILED",
                result.chosenMethod(),
                false,
                null,
                true,
                result.alternativeMethods()
        );
    }

    /**
     * Creates a response from BLL result (auto-detects success/failure).
     *
     * @param result the BLL result
     * @return appropriate response
     */
    public static TwoFactorMethodChosenResponse from(TwoFactorMethodChosenResult result) {
        if (result.deliveryFailed()) {
            return deliveryFailed(result);
        }
        return success(result);
    }
}