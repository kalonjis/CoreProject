package be.steby.CoreProject.il.webhook.mapper;

import be.steby.CoreProject.bll.domains.lead.models.LeadManualCreateRequest;
import be.steby.CoreProject.il.webhook.config.WebhookProperties.WebhookSourceConfig;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Strategy for mapping a platform-specific webhook payload to a normalized
 * {@link LeadManualCreateRequest}.
 *
 * <p>Each implementation handles one external platform. Implementations are
 * Spring beans auto-discovered by {@link be.steby.CoreProject.il.webhook.registry.WebhookHandlerRegistry}
 * via the {@link #source()} key, which must match the YAML config key
 * (e.g. {@code "facebook"}, {@code "typeform"}).</p>
 *
 * <p>Returns {@link Optional#empty()} when the payload is not a lead event
 * (e.g. Facebook sends non-lead notifications on the same webhook).</p>
 */
public interface WebhookMapper {

    /** Unique source key matching the YAML config key (e.g. {@code "facebook"}). */
    String source();

    /**
     * Maps the raw payload to a {@link LeadManualCreateRequest}.
     *
     * @param payload the parsed JSON body
     * @param config  the platform configuration (secret, extras, defaultLeadType…)
     * @return the normalized request, or empty if this payload is not a lead event
     */
    Optional<LeadManualCreateRequest> map(JsonNode payload, WebhookSourceConfig config);
}
