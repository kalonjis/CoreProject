package be.steby.CoreProject.il.webhook.verifier;

import be.steby.CoreProject.il.webhook.config.WebhookProperties.WebhookSourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Verifies webhook authenticity by comparing a plain shared token.
 *
 * <p>Used for platforms like Zapier or Make where you control the HTTP call
 * and can set any header value. The header value is compared against the
 * configured {@code secret} using a constant-time comparison to prevent
 * timing attacks.</p>
 */
@Component
@Slf4j
public class PlainTokenVerifier implements WebhookVerifier {

    @Override
    public String algorithm() {
        return "PLAIN_TOKEN";
    }

    @Override
    public boolean verify(byte[] rawBody, String signatureHeaderValue, WebhookSourceConfig config) {
        if (signatureHeaderValue == null || signatureHeaderValue.isBlank()) {
            log.warn("Plain token verification failed: missing header");
            return false;
        }

        boolean valid = MessageDigest.isEqual(
                config.getSecret().getBytes(StandardCharsets.UTF_8),
                signatureHeaderValue.getBytes(StandardCharsets.UTF_8)
        );

        if (!valid) {
            log.warn("Plain token verification failed: token mismatch");
        }
        return valid;
    }
}
