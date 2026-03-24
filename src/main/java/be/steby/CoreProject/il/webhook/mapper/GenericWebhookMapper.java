package be.steby.CoreProject.il.webhook.mapper;

import be.steby.CoreProject.bll.domains.lead.models.LeadManualCreateRequest;
import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.il.webhook.config.WebhookProperties.WebhookSourceConfig;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Generic flat-JSON webhook mapper for Make, Zapier, or any custom integration.
 *
 * <p>Expects a flat JSON body with standard field names.
 * Recommended payload structure:</p>
 * <pre>
 * {
 *   "email":      "prospect@example.com",
 *   "first_name": "Jean",
 *   "last_name":  "Dupont",
 *   "phone":      "+32 470 00 00 00",
 *   "company":    "ACME",
 *   "subject":    "Demande de devis",
 *   "message":    "Bonjour, je souhaite..."
 * }
 * </pre>
 *
 * <p>Also accepts {@code full_name} (split on first space) and {@code name}
 * as alternatives to {@code first_name}/{@code last_name}.</p>
 */
@Component
@Slf4j
public class GenericWebhookMapper implements WebhookMapper {

    @Override
    public String source() {
        return "generic";
    }

    @Override
    public Optional<LeadManualCreateRequest> map(JsonNode payload, WebhookSourceConfig config) {
        String email     = text(payload, "email");
        String firstName = text(payload, "first_name");
        String lastName  = text(payload, "last_name");
        String fullName  = text(payload, "full_name", "name");
        String phone     = text(payload, "phone", "phone_number");
        String company   = text(payload, "company", "company_name", "organisation");
        String subject   = text(payload, "subject");
        String message   = text(payload, "message", "notes", "body");

        if (email == null || email.isBlank()) {
            log.warn("Generic webhook: missing 'email' field");
            return Optional.empty();
        }

        if (firstName == null && fullName != null) {
            int sp = fullName.indexOf(' ');
            firstName = sp > 0 ? fullName.substring(0, sp) : fullName;
            lastName  = sp > 0 ? fullName.substring(sp + 1) : null;
        }

        if (subject == null || subject.isBlank()) {
            subject = "Lead externe — " + email;
        }

        LeadType leadType = resolveLeadType(config.getDefaultLeadType());

        return Optional.of(new LeadManualCreateRequest(
                email, null, firstName, lastName, phone, company, subject, message, leadType
        ));
    }

    /** Returns the first non-blank value found among the given field names. */
    private String text(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode n = node.path(field);
            if (!n.isMissingNode() && !n.isNull()) {
                String v = n.asText("").trim();
                if (!v.isEmpty()) return v;
            }
        }
        return null;
    }

    private LeadType resolveLeadType(String name) {
        try { return LeadType.valueOf(name); }
        catch (Exception e) { return LeadType.COMMERCIAL; }
    }
}
