package be.steby.CoreProject.il.webhook.mapper;

import be.steby.CoreProject.bll.domains.lead.models.LeadManualCreateRequest;
import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.il.webhook.config.WebhookProperties.WebhookSourceConfig;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Maps Typeform webhook payloads to {@link LeadManualCreateRequest}.
 *
 * <p>Typeform sends the full form response in the webhook body — no second API call needed.</p>
 *
 * <h3>Field matching strategy</h3>
 * <p>Fields are matched by answer {@code type} and by the field {@code ref}
 * (the custom reference set in the Typeform form builder).
 * Recommended refs in your Typeform:
 * <ul>
 *   <li>{@code email} — email type question</li>
 *   <li>{@code first_name}, {@code last_name} or {@code full_name} — short text</li>
 *   <li>{@code phone} or {@code phone_number} — phone type</li>
 *   <li>{@code company} or {@code company_name} — short text</li>
 *   <li>{@code subject} — short text (optional)</li>
 *   <li>{@code message} — long text (optional)</li>
 * </ul>
 */
@Component
@Slf4j
public class TypeformWebhookMapper implements WebhookMapper {

    @Override
    public String source() {
        return "typeform";
    }

    @Override
    public Optional<LeadManualCreateRequest> map(JsonNode payload, WebhookSourceConfig config) {
        JsonNode answers = payload.path("form_response").path("answers");
        if (answers.isMissingNode() || !answers.isArray()) {
            log.warn("Typeform webhook: missing form_response.answers");
            return Optional.empty();
        }

        String email = null, firstName = null, lastName = null,
               phone = null, company  = null, subject  = null,
               message = null, fullName = null;

        for (JsonNode answer : answers) {
            String ref  = answer.path("field").path("ref").asText("");
            String type = answer.path("type").asText("");

            String value = switch (type) {
                case "email"       -> answer.path("email").asText(null);
                case "phone_number"-> answer.path("phone_number").asText(null);
                case "text"        -> answer.path("text").asText(null);
                case "long_text"   -> answer.path("text").asText(null);
                case "short_text"  -> answer.path("text").asText(null);
                default            -> null;
            };

            if (value == null) continue;

            // Match by explicit ref first, then by answer type as fallback
            switch (ref) {
                case "email"                       -> email    = value;
                case "first_name"                  -> firstName = value;
                case "last_name"                   -> lastName  = value;
                case "full_name"                   -> fullName  = value;
                case "phone", "phone_number"       -> phone    = value;
                case "company", "company_name"     -> company  = value;
                case "subject"                     -> subject  = value;
                case "message", "notes"            -> message  = value;
                default -> {
                    // Fallback: assign by answer type if not yet found
                    if ("email".equals(type) && email == null)    email   = value;
                    if ("phone_number".equals(type) && phone == null) phone = value;
                }
            }
        }

        if (email == null || email.isBlank()) {
            log.warn("Typeform webhook: no email found in answers");
            return Optional.empty();
        }

        // full_name fallback
        if (firstName == null && fullName != null) {
            int sp = fullName.indexOf(' ');
            firstName = sp > 0 ? fullName.substring(0, sp) : fullName;
            lastName  = sp > 0 ? fullName.substring(sp + 1) : null;
        }

        if (subject == null || subject.isBlank()) {
            subject = "Lead Typeform — " + email;
        }

        LeadType leadType = resolveLeadType(config.getDefaultLeadType());

        return Optional.of(new LeadManualCreateRequest(
                email, null, firstName, lastName, phone, company, subject, message, leadType
        ));
    }

    private LeadType resolveLeadType(String name) {
        try { return LeadType.valueOf(name); }
        catch (Exception e) { return LeadType.COMMERCIAL; }
    }
}
