package be.steby.CoreProject.pl.domains.auth.models.responses;

import lombok.Builder;

/**
 * Response DTO for TOTP setup endpoint.
 * Contains all information needed to configure authenticator apps.
 */
@Builder
public record TOTPSetupResponse(

        /**
         * Plain TOTP secret for manual entry in authenticator apps
         * Example: "JBSWY3DPEHPK3PXP"
         */
        String secret,

        /**
         * Complete TOTP URL for QR code generation
         * Example: "otpauth://totp/YourApp:user@email.com?secret=JBSWY3DPEHPK3PXP&issuer=YourApp"
         */
        String qrCodeUrl,

        /**
         * QR code as base64 image (optional for development)
         * Example: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
         */
        String qrCodeImage,

        /**
         * Account name displayed in authenticator app
         * Example: "user@email.com"
         */
        String accountName,

        /**
         * Issuer name displayed in authenticator app
         * Example: "YourApp"
         */
        String issuer,

        /**
         * Instructions for manual setup
         */
        String manualSetupInstructions
) {

    /**
     * Factory method for creating setup response
     */
    public static TOTPSetupResponse create(String secret, String qrCodeUrl,
                                           String qrCodeImage, String accountName,
                                           String issuer) {
        return TOTPSetupResponse.builder()
                .secret(secret)
                .qrCodeUrl(qrCodeUrl)
                .qrCodeImage(qrCodeImage)
                .accountName(accountName)
                .issuer(issuer)
                .manualSetupInstructions(
                        "Open Google Authenticator > + > Enter a setup key > Paste the secret above"
                )
                .build();
    }
}