package be.steby.CoreProject.bll.domains.password.models;

import be.steby.CoreProject.dl.enums.PasswordResetType;

/**
 * Result of initiating a code-based password reset (EMAIL_CODE or SMS_CODE).
 *
 * @param success   whether the password reset was initiated successfully
 * @param jwtToken  JWT token containing token reference for verification (null if failed or link-based)
 * @param phoneHint masked phone number hint for UX feedback (SMS_CODE only, null otherwise)
 *
 * @see PasswordResetType
 * @see CodeVerificationResult
 */
public record CodePasswordResetResult(
        boolean success,
        String jwtToken,
        String phoneHint
) {

    /**
     * Creates a successful result with JWT token (EMAIL_CODE flow).
     */
    public static CodePasswordResetResult success(String jwtToken) {
        return new CodePasswordResetResult(true, jwtToken, null);
    }

    /**
     * Creates a successful result with JWT token and phone hint (SMS_CODE flow).
     *
     * <p>The phone hint allows the frontend to display a masked number
     * (e.g. "+32 *** *** 47") to confirm which number the code was sent to.
     *
     * @param jwtToken  the JWT token to be stored in verification cookie
     * @param phoneHint masked phone number for display (e.g. "+32 *** *** 47")
     */
    public static CodePasswordResetResult success(String jwtToken, String phoneHint) {
        return new CodePasswordResetResult(true, jwtToken, phoneHint);
    }

    /**
     * Creates a result indicating link-based reset was used (EMAIL_LINK flow).
     */
    public static CodePasswordResetResult linkBased() {
        return new CodePasswordResetResult(true, null, null);
    }

    /**
     * Creates a failed result (silent failure for security).
     */
    public static CodePasswordResetResult failure() {
        return new CodePasswordResetResult(false, null, null);
    }

    public boolean hasToken() {
        return jwtToken != null && !jwtToken.isBlank();
    }

    public boolean hasPhoneHint() {
        return phoneHint != null && !phoneHint.isBlank();
    }
}