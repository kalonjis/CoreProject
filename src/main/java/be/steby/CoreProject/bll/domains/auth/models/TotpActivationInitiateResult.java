package be.steby.CoreProject.bll.domains.auth.models;

/**
 * Record representing the result of a TOTP two-factor authentication activation initiation.
 *
 * This immutable data structure contains all information needed to:
 * - Display QR code to user for authenticator app setup
 * - Provide manual entry key as fallback
 * - Store activation state in secure cookie (JWT token)
 *
 * Unlike email 2FA activation which sends a code via email, TOTP activation
 * generates a secret key that the user scans/enters into their authenticator app.
 * The activation token contains the hashed secret for verification in step 2.
 *
 * @param qrCodeUri the otpauth:// URI for QR code generation (e.g., otpauth://totp/App:user@email.com?secret=XXX&issuer=App)
 * @param secretKey the Base32-encoded secret key for the authenticator app
 * @param manualEntryKey the formatted secret key for manual entry (grouped for readability)
 * @param twoFaActivationToken the JWT token containing hashed secret and user info for step 2 verification
 *
 * @author Steby Team
 * @since 2.0.0
 */
public record TotpActivationInitiateResult(
        String qrCodeUri,
        String secretKey,
        String manualEntryKey,
        String twoFaActivationToken
) {

    /**
     * Validates the activation result parameters.
     *
     * @throws IllegalArgumentException if any required parameter is null or invalid
     */
    public TotpActivationInitiateResult {
        if (qrCodeUri == null || qrCodeUri.isBlank()) {
            throw new IllegalArgumentException("QR code URI cannot be null or blank");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("Secret key cannot be null or blank");
        }
        if (manualEntryKey == null || manualEntryKey.isBlank()) {
            throw new IllegalArgumentException("Manual entry key cannot be null or blank");
        }
        if (twoFaActivationToken == null || twoFaActivationToken.isBlank()) {
            throw new IllegalArgumentException("Two factor activation token cannot be null or blank");
        }
    }
}