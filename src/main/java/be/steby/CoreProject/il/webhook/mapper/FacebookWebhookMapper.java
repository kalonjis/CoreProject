package be.steby.CoreProject.il.webhook.mapper;

import be.steby.CoreProject.bll.domains.crm.lead.models.LeadManualCreateRequest;
import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.il.webhook.config.WebhookProperties.WebhookSourceConfig;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Maps Facebook Lead Ads webhook payloads to {@link LeadManualCreateRequest}.
 *
 * <h3>Two-step flow</h3>
 * <p>Facebook sends only a notification containing a {@code leadgen_id}.
 * This mapper fetches the actual lead data from the Graph API using that ID
 * and the configured {@code pageAccessToken}.</p>
 *
 * <h3>Expected field refs in the Facebook form</h3>
 * <ul>
 *   <li>{@code email} — prospect email</li>
 *   <li>{@code full_name} or {@code first_name} + {@code last_name}</li>
 *   <li>{@code phone_number}</li>
 *   <li>{@code company_name}</li>
 * </ul>
 *
 * <p>Required extra config key: {@code pageAccessToken}, {@code graphApiVersion}
 * (defaults to {@code v19.0}).</p>
 */
@Component
@Slf4j
public class FacebookWebhookMapper implements WebhookMapper {

    private static final String GRAPH_BASE = "https://graph.facebook.com";

    private final RestClient restClient;

    public FacebookWebhookMapper(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(GRAPH_BASE).build();
    }

    @Override
    public String source() {
        return "facebook";
    }

    @Override
    public Optional<LeadManualCreateRequest> map(JsonNode payload, WebhookSourceConfig config) {
        // Facebook sends: { "object": "page", "entry": [{ "changes": [{ "field": "leadgen", "value": { "leadgen_id": "..." } }] }] }
        JsonNode leadgenId = payload
                .path("entry").path(0)
                .path("changes").path(0)
                .path("value").path("leadgen_id");

        if (leadgenId.isMissingNode()) {
            log.debug("Facebook webhook: not a leadgen event, skipping");
            return Optional.empty();
        }

        String id = leadgenId.asText();
        log.info("Facebook webhook: fetching leadgen_id={}", id);

        return fetchLeadData(id, config);
    }

    private Optional<LeadManualCreateRequest> fetchLeadData(String leadgenId, WebhookSourceConfig config) {
        String pageAccessToken = config.getExtra().getOrDefault("pageAccessToken", "");
        String apiVersion      = config.getExtra().getOrDefault("graphApiVersion", "v19.0");

        try {
            JsonNode response = restClient.get()
                    .uri("/{version}/{id}?access_token={token}&fields=field_data",
                            apiVersion, leadgenId, pageAccessToken)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) return Optional.empty();

            return Optional.of(parseFieldData(response.path("field_data"), config));

        } catch (Exception e) {
            log.error("Failed to fetch Facebook lead data for leadgen_id={}", leadgenId, e);
            return Optional.empty();
        }
    }

    /**
     * Maps Facebook's {@code field_data} array (list of {@code {name, values[]}}) to the request model.
     * Field names are the "refs" set in the Facebook Lead Ad form builder.
     */
    private LeadManualCreateRequest parseFieldData(JsonNode fieldData, WebhookSourceConfig config) {
        String email     = null, firstName = null, lastName = null,
               phone     = null, company   = null, fullName = null;

        for (JsonNode field : fieldData) {
            String name  = field.path("name").asText();
            String value = field.path("values").path(0).asText(null);
            switch (name) {
                case "email"        -> email     = value;
                case "first_name"   -> firstName = value;
                case "last_name"    -> lastName  = value;
                case "full_name"    -> fullName  = value;
                case "phone_number" -> phone     = value;
                case "company_name" -> company   = value;
                default             -> { /* ignore unknown fields */ }
            }
        }

        // full_name fallback: split on first space
        if (firstName == null && fullName != null) {
            int sp = fullName.indexOf(' ');
            firstName = sp > 0 ? fullName.substring(0, sp) : fullName;
            lastName  = sp > 0 ? fullName.substring(sp + 1) : null;
        }

        String subject = buildSubject(firstName, lastName, email);
        LeadType leadType = resolveLeadType(config.getDefaultLeadType());

        return new LeadManualCreateRequest(
                email != null ? email : "",
                null,        // civility not available from Facebook
                firstName,
                lastName,
                phone,
                company,
                subject,
                null,        // no message body from Facebook Lead Ads
                leadType
        );
    }

    private String buildSubject(String firstName, String lastName, String email) {
        if (firstName != null || lastName != null) {
            return "Lead Facebook — " + trim(firstName) + " " + trim(lastName);
        }
        return "Lead Facebook — " + (email != null ? email : "");
    }

    private String trim(String s) { return s != null ? s.trim() : ""; }

    private LeadType resolveLeadType(String name) {
        try { return LeadType.valueOf(name); }
        catch (Exception e) { return LeadType.COMMERCIAL; }
    }
}
