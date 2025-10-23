package be.steby.CoreProject.bll.domains.profile.events;

/**
 * Event triggered when SMS verification process is initiated.
 * Contains the plain text phone number and verification code for SMS sending.
 *
 * This event is published when:
 * - User requests SMS verification
 * - Phone number is validated
 * - Verification code is generated
 *
 * The event will be handled by notification listeners to send the SMS.
 *
 * @param phoneNumber The phone number in international format (plain text)
 * @param verificationCode The 6-digit verification code (plain text)
 */
public record PhoneVerificationInitiatedEvent(
        String phoneNumber,
        String verificationCode
) {
}