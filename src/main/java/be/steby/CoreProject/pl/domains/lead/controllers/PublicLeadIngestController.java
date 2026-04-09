package be.steby.CoreProject.pl.domains.lead.controllers;

import be.steby.CoreProject.bll.domains.crm.lead.models.LeadManualCreateRequest;
import be.steby.CoreProject.bll.domains.crm.lead.services.LeadService;
import be.steby.CoreProject.dl.enums.crm.LeadSource;
import be.steby.CoreProject.il.webhook.config.WebhookProperties.WebhookSourceConfig;
import be.steby.CoreProject.il.webhook.registry.WebhookHandlerRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Public endpoint for ingesting leads from external platforms via webhook.
 *
 * <p>URL: {@code /api/public/leads/ingest/{source}}</p>
 *
 * <p>Two operations:</p>
 * <ul>
 *   <li>{@code GET /{source}} — platform challenge handshake (Facebook, Twitter)</li>
 *   <li>{@code POST /{source}} — receive and process a lead payload</li>
 * </ul>
 *
 * <p>Security: each request is verified against the platform's configured
 * algorithm (HMAC_SHA256 or PLAIN_TOKEN) before any processing occurs.</p>
 */
@RestController
@RequestMapping("/api/public/leads/ingest")
@RequiredArgsConstructor
@Slf4j
public class PublicLeadIngestController {

    private final WebhookHandlerRegistry registry;
    private final LeadService            leadService;
    private final ObjectMapper           objectMapper;

    // =========================================================================
    // GET — Challenge / handshake (Facebook, Twitter CRC)
    // =========================================================================

    /**
     * Handles the one-time webhook subscription verification sent by Facebook and similar platforms.
     *
     * <p>Facebook sends: {@code GET ?hub.mode=subscribe&hub.verify_token=xxx&hub.challenge=yyy}</p>
     * <p>We respond with the raw {@code hub.challenge} value if the verify token matches.</p>
     */
    @GetMapping("/{source}")
    public ResponseEntity<String> challenge(
            @PathVariable String source,
            @RequestParam(value = "hub.mode",         required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(value = "hub.challenge",    required = false) String challenge
    ) {
        Optional<WebhookSourceConfig> configOpt = registry.findConfig(source);
        if (configOpt.isEmpty()) {
            log.warn("Webhook challenge: unknown source '{}'", source);
            return ResponseEntity.notFound().build();
        }

        WebhookSourceConfig config = configOpt.get();

        // Only Facebook-style challenge is supported for now
        if (!"subscribe".equals(mode) || challenge == null) {
            return ResponseEntity.badRequest().body("Unsupported challenge format");
        }

        String expectedToken = config.getVerifyToken();
        if (expectedToken == null || !expectedToken.equals(verifyToken)) {
            log.warn("Webhook challenge '{}': verify_token mismatch", source);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid verify_token");
        }

        log.info("Webhook challenge '{}': verified successfully", source);
        return ResponseEntity.ok(challenge);
    }

    // =========================================================================
    // POST — Lead ingest
    // =========================================================================

    /**
     * Receives a webhook payload from an external platform and creates a CRM lead.
     *
     * <p>Processing steps:</p>
     * <ol>
     *   <li>Resolve configuration for the given {@code source}</li>
     *   <li>Verify the request signature (HMAC or plain token)</li>
     *   <li>Parse and map the payload to a {@link LeadManualCreateRequest}</li>
     *   <li>Persist the lead via {@link LeadService#createFromWebhook}</li>
     * </ol>
     *
     * <p>Always returns {@code 200 OK} on signature failure to avoid leaking
     * information to potential attackers. Legitimate failures are logged.</p>
     */
    @PostMapping("/{source}")
    public ResponseEntity<Void> ingest(
            @PathVariable String source,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String fbSignature,
            @RequestHeader(value = "Typeform-Signature",   required = false) String typeformSignature,
            @RequestHeader(value = "X-LinkedIn-Signature", required = false) String linkedinSignature,
            @RequestHeader(value = "X-Webhook-Token",      required = false) String genericToken,
            @RequestBody byte[] rawBody
    ) {
        Optional<WebhookSourceConfig> configOpt = registry.findConfig(source);
        if (configOpt.isEmpty()) {
            log.warn("Webhook ingest: unknown source '{}'", source);
            return ResponseEntity.notFound().build();
        }

        WebhookSourceConfig config = configOpt.get();

        // 1. Resolve the signature header value for this source
        String signatureValue = resolveSignatureHeader(config, source,
                fbSignature, typeformSignature, linkedinSignature, genericToken);

        // 2. Verify signature
        boolean verified = registry.findVerifier(config)
                .map(v -> v.verify(rawBody, signatureValue, config))
                .orElse(false);

        if (!verified) {
            log.warn("Webhook ingest '{}': signature verification failed — request ignored", source);
            // Return 200 to avoid leaking verification details to attackers
            return ResponseEntity.ok().build();
        }

        // 3. Parse payload
        JsonNode payload;
        try {
            payload = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            log.error("Webhook ingest '{}': failed to parse JSON body", source, e);
            return ResponseEntity.badRequest().build();
        }

        // 4. Map to LeadManualCreateRequest
        Optional<LeadManualCreateRequest> requestOpt = registry.findMapper(source)
                .flatMap(mapper -> mapper.map(payload, config));

        if (requestOpt.isEmpty()) {
            // Not a lead event (e.g. Facebook sends page events on the same webhook)
            log.debug("Webhook ingest '{}': payload skipped (not a lead event or mapping failed)", source);
            return ResponseEntity.ok().build();
        }

        // 5. Persist
        LeadSource leadSource = resolveLeadSource(config.getLeadSource(), source);
        leadService.createFromWebhook(requestOpt.get(), leadSource);

        log.info("Webhook ingest '{}': lead created successfully", source);
        return ResponseEntity.ok().build();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Picks the appropriate signature header value based on the source config's
     * {@code signatureHeader} field.
     */
    private String resolveSignatureHeader(
            WebhookSourceConfig config, String source,
            String fbSignature, String typeformSignature,
            String linkedinSignature, String genericToken
    ) {
        return switch (config.getSignatureHeader()) {
            case "X-Hub-Signature-256"   -> fbSignature;
            case "Typeform-Signature"     -> typeformSignature;
            case "X-LinkedIn-Signature"   -> linkedinSignature;
            case "X-Webhook-Token"        -> genericToken;
            default -> {
                log.warn("Webhook '{}': unrecognised signatureHeader '{}'", source, config.getSignatureHeader());
                yield null;
            }
        };
    }

    private LeadSource resolveLeadSource(String name, String fallback) {
        try {
            return LeadSource.valueOf(name);
        } catch (Exception e) {
            log.warn("Unknown leadSource '{}' for webhook source '{}', using OTHER", name, fallback);
            return LeadSource.OTHER;
        }
    }
}
