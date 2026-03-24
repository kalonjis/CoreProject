package be.steby.CoreProject.il.webhook.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Typed configuration for external webhook sources.
 *
 * <p>Bound from {@code crm.webhooks.sources.*} in {@code crm_webhooks.yml}.</p>
 *
 * <p>Each key in {@code sources} matches the {@code {source}} path variable
 * of {@code /api/public/leads/ingest/{source}}.</p>
 *
 * <h4>Configuration example:</h4>
 * <pre>{@code
 * crm:
 *   webhooks:
 *     sources:
 *       facebook:
 *         secret: ${WEBHOOK_SECRET_FACEBOOK}
 *         signature-header: X-Hub-Signature-256
 *         algorithm: HMAC_SHA256
 *         verify-token: ${WEBHOOK_VERIFY_TOKEN_FACEBOOK}
 *         lead-source: FACEBOOK_ADS
 *         default-lead-type: COMMERCIAL
 *         extra:
 *           pageAccessToken: ${FACEBOOK_PAGE_ACCESS_TOKEN}
 * }</pre>
 */
@Component
@ConfigurationProperties(prefix = "crm.webhooks")
@Getter
@Setter
public class WebhookProperties {

    private Map<String, WebhookSourceConfig> sources = new HashMap<>();

    /**
     * Configuration for a single external platform source.
     */
    @Getter
    @Setter
    public static class WebhookSourceConfig {

        /** Shared secret used to verify the request signature. */
        private String secret;

        /** HTTP header name carrying the platform's signature (e.g. {@code X-Hub-Signature-256}). */
        private String signatureHeader;

        /** Verification algorithm: {@code HMAC_SHA256} or {@code PLAIN_TOKEN}. */
        private String algorithm;

        /**
         * Challenge token for platforms that require a GET handshake (Facebook, Twitter).
         * Optional — only needed when the platform sends a verify_token.
         */
        private String verifyToken;

        /** {@link be.steby.CoreProject.dl.enums.crm.LeadSource} enum name for created leads. */
        private String leadSource;

        /**
         * {@link be.steby.CoreProject.dl.enums.LeadType} enum name applied when the platform
         * does not specify a type. Defaults to {@code COMMERCIAL}.
         */
        private String defaultLeadType = "COMMERCIAL";

        /**
         * Platform-specific extra parameters.
         * Example: {@code pageAccessToken} for the Facebook Graph API call.
         */
        private Map<String, String> extra = new HashMap<>();
    }
}
