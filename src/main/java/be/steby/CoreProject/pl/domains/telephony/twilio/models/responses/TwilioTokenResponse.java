package be.steby.CoreProject.pl.domains.telephony.twilio.models.responses;

/**
 * Response containing a short-lived Twilio Access Token.
 *
 * <p>The {@code token} is a signed JWT that the frontend passes to
 * {@code new Device(token)} from {@code @twilio/voice-sdk}. It expires after one hour.</p>
 */
public record TwilioTokenResponse(String token) {}
