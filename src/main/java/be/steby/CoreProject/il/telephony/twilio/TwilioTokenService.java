package be.steby.CoreProject.il.telephony.twilio;

import be.steby.CoreProject.dl.entities.User;
import com.twilio.jwt.accesstoken.AccessToken;
import com.twilio.jwt.accesstoken.VoiceGrant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Generates short-lived Twilio Access Tokens for the frontend Twilio Voice SDK.
 *
 * <p>Credentials are read from {@code telephony.yml} (env vars {@code TWILIO_ACCOUNT_SID},
 * {@code TWILIO_API_KEY_SID}, {@code TWILIO_API_KEY_SECRET}, {@code TWILIO_TWIML_APP_SID}).
 * The commercial's {@code publicId} is used as the Twilio identity.</p>
 */
@Slf4j
@Service
public class TwilioTokenService {

    private static final int TOKEN_TTL_SECONDS = 3600;

    @Value("${telephony.twilio.account-sid:}")
    private String accountSid;

    @Value("${telephony.twilio.api-key-sid:}")
    private String apiKeySid;

    @Value("${telephony.twilio.api-key-secret:}")
    private String apiKeySecret;

    @Value("${telephony.twilio.twiml-app-sid:}")
    private String twimlAppSid;

    /**
     * Generates a Twilio Access Token for the given user (outbound-only).
     *
     * @param user the authenticated commercial requesting the token
     * @return a signed JWT passed to {@code new Device(token)} on the frontend
     */
    public String generateAccessToken(User user) {
        ensureConfigured();

        VoiceGrant grant = new VoiceGrant();
        grant.setOutgoingApplicationSid(twimlAppSid);
        grant.setIncomingAllow(false);

        AccessToken token = new AccessToken.Builder(
                accountSid,
                apiKeySid,
                apiKeySecret.getBytes(StandardCharsets.UTF_8)
        )
        .identity(user.getPublicId())
        .ttl(TOKEN_TTL_SECONDS)
        .grant(grant)
        .build();

        log.debug("Twilio Access Token generated for user: {}", user.getId());
        return token.toJwt();
    }

    /**
     * Generates a Twilio Access Token for the test-receiver HTML page (dev only).
     * Incoming calls are allowed so the page can receive calls from the CRM.
     */
    public String generateTestReceiverToken() {
        ensureConfigured();

        VoiceGrant grant = new VoiceGrant();
        grant.setOutgoingApplicationSid(twimlAppSid);
        grant.setIncomingAllow(true);

        AccessToken token = new AccessToken.Builder(
                accountSid,
                apiKeySid,
                apiKeySecret.getBytes(StandardCharsets.UTF_8)
        )
        .identity("test-receiver")
        .ttl(TOKEN_TTL_SECONDS)
        .grant(grant)
        .build();

        log.debug("Twilio test-receiver token generated");
        return token.toJwt();
    }

    private void ensureConfigured() {
        if (accountSid == null || accountSid.isBlank()) {
            throw new IllegalStateException(
                    "Twilio credentials not configured — set TWILIO_ACCOUNT_SID in environment");
        }
    }
}
