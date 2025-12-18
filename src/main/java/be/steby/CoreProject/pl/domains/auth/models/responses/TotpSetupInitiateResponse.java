package be.steby.CoreProject.pl.domains.auth.models.responses;

import be.steby.CoreProject.bll.domains.auth.models.TotpActivationInitiateResult;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import lombok.Builder;

/**
 * Response model for TOTP 2FA setup initiation (step 1 of 2-step activation flow).
 *
 * Contains all information needed for the frontend to:
 * - Display the QR code for authenticator app scanning
 * - Show the manual entry key as fallback option
 * - Guide the user through the setup process
 *
 * The activation token is NOT included here as it is set as an HttpOnly cookie
 * by the controller for security reasons.
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Builder
public record TotpSetupInitiateResponse(
        String message,
        String type,
        String status,
        String qrCodeUri,
        String secretKey,
        String manualEntryKey,
        String instructions
) {

    /**
     * Factory method to create a successful initiation response from BLL result.
     *
     * @param result the BLL result containing QR code and secret information
     * @return TotpSetupInitiateResponse ready for HTTP response
     */
    public static TotpSetupInitiateResponse fromBllResult(TotpActivationInitiateResult result) {
        return TotpSetupInitiateResponse.builder()
                .message("TOTP two-factor authentication setup initiated successfully. Please scan the QR code with your authenticator app.")
                .type(TwoFactorType.TOTP.name())
                .status("activation_initiated")
                .qrCodeUri(result.qrCodeUri())
                .secretKey(result.secretKey())
                .manualEntryKey(result.manualEntryKey())
                .instructions("Scan the QR code with your authenticator app (Google Authenticator, Authy, etc.) or manually enter the secret key. Then enter the 6-digit code to verify.")
                .build();
    }
}