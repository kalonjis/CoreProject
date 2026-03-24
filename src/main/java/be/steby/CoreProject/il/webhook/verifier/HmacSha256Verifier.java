package be.steby.CoreProject.il.webhook.verifier;

import be.steby.CoreProject.il.webhook.config.WebhookProperties.WebhookSourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Verifies webhook signatures using HMAC-SHA256.
 *
 * <p>Covers Facebook, Typeform, LinkedIn, GitHub, Stripe and most
 * modern webhook providers. The expected signature format is
 * {@code sha256=<hex-digest>} (with or without the prefix).</p>
 */
@Component
@Slf4j
public class HmacSha256Verifier implements WebhookVerifier {

    @Override
    public String algorithm() {
        return "HMAC_SHA256";
    }

    @Override
    public boolean verify(byte[] rawBody, String signatureHeaderValue, WebhookSourceConfig config) {
        if (signatureHeaderValue == null || signatureHeaderValue.isBlank()) {
            log.warn("HMAC verification failed: missing signature header");
            return false;
        }

        try {
            String expected = computeHmac(rawBody, config.getSecret());
            // Strip prefix (sha256=, sha1=, etc.) if present
            String received = signatureHeaderValue.contains("=")
                    ? signatureHeaderValue.substring(signatureHeaderValue.indexOf('=') + 1)
                    : signatureHeaderValue;

            boolean valid = MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    received.getBytes(StandardCharsets.UTF_8)
            );

            if (!valid) {
                log.warn("HMAC verification failed: signature mismatch");
            }
            return valid;

        } catch (Exception e) {
            log.error("HMAC verification error", e);
            return false;
        }
    }

    private String computeHmac(byte[] data, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data);
        // Encode as lowercase hex
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
