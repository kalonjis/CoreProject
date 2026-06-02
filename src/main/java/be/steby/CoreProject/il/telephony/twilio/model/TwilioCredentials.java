package be.steby.CoreProject.il.telephony.twilio.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Typed representation of Twilio credentials stored (AES-256/GCM encrypted) in
 * {@link be.steby.CoreProject.dl.entities.crm.TelephonyConfig#getEncryptedCredentials()}.
 *
 * <h3>Admin setup</h3>
 * <p>Create the TelephonyConfig via {@code POST /api/crm/telephony/configs} with provider {@code TWILIO}
 * and the following credentials JSON:</p>
 * <pre>{@code
 * {
 *   "accountSid":   "ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
 *   "authToken":    "your_auth_token",
 *   "apiKeySid":    "SKxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
 *   "apiKeySecret": "your_api_key_secret",
 *   "twimlAppSid":  "APxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
 * }
 * }</pre>
 *
 * <ul>
 *   <li>{@code accountSid} / {@code authToken} — Twilio account credentials for signature validation</li>
 *   <li>{@code apiKeySid} / {@code apiKeySecret} — API Key (not Auth Token) for safer Access Token generation</li>
 *   <li>{@code twimlAppSid} — Twilio TwiML Application whose Voice URL points to
 *       {@code POST /api/crm/telephony/twilio/twiml} and Status Callback URL to
 *       {@code POST /api/crm/telephony/twilio/webhook}</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TwilioCredentials(
        String accountSid,
        String authToken,
        String apiKeySid,
        String apiKeySecret,
        String twimlAppSid
) {}
