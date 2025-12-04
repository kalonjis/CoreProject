package be.steby.CoreProject.dl.enums;

/**
 * Enumeration of password reset delivery methods.
 *
 * <p>This enum defines the available methods for delivering password reset
 * credentials to users. Each method has different security characteristics,
 * user experience implications, and technical requirements.
 *
 * <p>The methods are divided into two categories:
 * <ul>
 *   <li><strong>Link-based:</strong> User clicks a link containing a token ({@link #EMAIL_LINK})</li>
 *   <li><strong>Code-based:</strong> User enters a 6-digit verification code ({@link #EMAIL_CODE}, {@link #SMS_CODE})</li>
 * </ul>
 *
 * <p>Code-based methods require an additional verification step before password reset,
 * providing an extra layer of security through short-lived codes.
 *
 * @see be.steby.CoreProject.pl.domains.password.models.requests.ForgotPasswordRequest
 * @see be.steby.CoreProject.bll.domains.password.services.PasswordService
 */
public enum PasswordResetType {

    /**
     * Email with clickable reset link.
     *
     * <p>Sends an email containing a URL with an embedded reset token.
     * The user clicks the link and is directed to the password reset page.
     *
     * <p><strong>Flow:</strong>
     * <ol>
     *   <li>User requests password reset</li>
     *   <li>System sends email with reset link</li>
     *   <li>User clicks link → redirected to reset-password page</li>
     *   <li>User enters new password</li>
     * </ol>
     *
     * <p><strong>Characteristics:</strong>
     * <ul>
     *   <li>Token embedded in URL query parameter</li>
     *   <li>Longer token validity (typically 24 hours)</li>
     *   <li>Single-step verification (click link)</li>
     *   <li>Works on any device with email access</li>
     * </ul>
     *
     * <p><strong>Requirements:</strong>
     * <ul>
     *   <li>Valid email address (always satisfied for registered users)</li>
     * </ul>
     */
    EMAIL_LINK,

    /**
     * Email with 6-digit verification code.
     *
     * <p>Sends an email containing a short numeric code that the user
     * must enter manually on the verification page.
     *
     * <p><strong>Flow:</strong>
     * <ol>
     *   <li>User requests password reset</li>
     *   <li>System sends email with 6-digit code</li>
     *   <li>User enters code on verify-code page</li>
     *   <li>System validates code → sets permission cookie</li>
     *   <li>User redirected to reset-password-code page</li>
     *   <li>User enters new password</li>
     * </ol>
     *
     * <p><strong>Characteristics:</strong>
     * <ul>
     *   <li>Short numeric code (6 digits)</li>
     *   <li>Shorter validity period (typically 10 minutes)</li>
     *   <li>Two-step verification (enter code, then reset)</li>
     *   <li>Permission granted via HTTP-only cookie</li>
     * </ul>
     *
     * <p><strong>Requirements:</strong>
     * <ul>
     *   <li>Valid email address (always satisfied for registered users)</li>
     * </ul>
     *
     * <p><strong>Security advantages:</strong>
     * <ul>
     *   <li>Code cannot be intercepted by viewing URL in browser history</li>
     *   <li>Shorter validity reduces exposure window</li>
     *   <li>Requires active user interaction</li>
     * </ul>
     */
    EMAIL_CODE,

    /**
     * SMS with 6-digit verification code.
     *
     * <p>Sends an SMS message containing a short numeric code that the user
     * must enter manually on the verification page.
     *
     * <p><strong>Flow:</strong>
     * <ol>
     *   <li>User requests password reset</li>
     *   <li>System sends SMS with 6-digit code</li>
     *   <li>User enters code on verify-code page</li>
     *   <li>System validates code → sets permission cookie</li>
     *   <li>User redirected to reset-password-code page</li>
     *   <li>User enters new password</li>
     * </ol>
     *
     * <p><strong>Characteristics:</strong>
     * <ul>
     *   <li>Short numeric code (6 digits)</li>
     *   <li>Shorter validity period (typically 10 minutes)</li>
     *   <li>Two-step verification (enter code, then reset)</li>
     *   <li>Permission granted via HTTP-only cookie</li>
     *   <li>Fastest delivery method</li>
     * </ul>
     *
     * <p><strong>Requirements:</strong>
     * <ul>
     *   <li>User must have a phone number configured</li>
     *   <li>Phone number must be verified</li>
     *   <li>SMS service must be available and configured</li>
     * </ul>
     *
     * <p><strong>Fallback behavior:</strong>
     * If requirements are not met, the system silently processes the request
     * without sending any notification (security: prevents user enumeration).
     */
    SMS_CODE;

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    /**
     * Checks if this reset type requires code verification.
     *
     * <p>Code-based methods ({@link #EMAIL_CODE}, {@link #SMS_CODE}) require
     * the user to enter a verification code before accessing the password
     * reset form. Link-based methods ({@link #EMAIL_LINK}) do not.
     *
     * @return {@code true} if this method requires code verification,
     *         {@code false} otherwise
     */
    public boolean requiresCodeVerification() {
        return this == EMAIL_CODE || this == SMS_CODE;
    }

    /**
     * Checks if this reset type uses email as the delivery channel.
     *
     * @return {@code true} if the reset credentials are sent via email,
     *         {@code false} otherwise
     */
    public boolean isEmailDelivery() {
        return this == EMAIL_LINK || this == EMAIL_CODE;
    }

    /**
     * Checks if this reset type uses SMS as the delivery channel.
     *
     * @return {@code true} if the reset credentials are sent via SMS,
     *         {@code false} otherwise
     */
    public boolean isSmsDelivery() {
        return this == SMS_CODE;
    }

    /**
     * Checks if this reset type requires a verified phone number.
     *
     * <p>Only SMS-based methods require a verified phone number.
     * Email-based methods work with any registered user.
     *
     * @return {@code true} if a verified phone number is required,
     *         {@code false} otherwise
     */
    public boolean requiresVerifiedPhone() {
        return this == SMS_CODE;
    }
}