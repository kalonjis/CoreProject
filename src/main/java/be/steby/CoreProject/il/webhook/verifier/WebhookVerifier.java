package be.steby.CoreProject.il.webhook.verifier;

import be.steby.CoreProject.il.webhook.config.WebhookProperties.WebhookSourceConfig;

/**
 * Strategy for verifying that an incoming webhook request is legitimate.
 *
 * <p>Each implementation handles a specific algorithm (HMAC_SHA256, PLAIN_TOKEN, …).
 * Implementations are Spring beans discovered and registered by {@link WebhookVerifierRegistry}.</p>
 */
public interface WebhookVerifier {

    /** Returns the algorithm name this verifier handles (e.g. {@code "HMAC_SHA256"}). */
    String algorithm();

    /**
     * Verifies the request signature.
     *
     * @param rawBody the raw request body bytes (needed for HMAC computation)
     * @param signatureHeaderValue the value of the signature header sent by the platform
     * @param config  the source configuration containing the secret and header name
     * @return {@code true} if the signature is valid
     */
    boolean verify(byte[] rawBody, String signatureHeaderValue, WebhookSourceConfig config);
}
