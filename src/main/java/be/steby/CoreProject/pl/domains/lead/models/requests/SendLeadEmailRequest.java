package be.steby.CoreProject.pl.domains.lead.models.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PL request model for sending a CRM outreach email to a lead.
 *
 * <p>Passed as the JSON body of {@code POST /api/crm/leads/:publicId/email}.
 * The {@code leadPublicId} is taken from the URL path parameter, not the body.</p>
 *
 * @param subject email subject written by the commercial (required, max 200 chars)
 * @param body    rich-text HTML produced by TipTap on the frontend (required, max 10 000 chars).
 *                Bypasses XSS deserializer via {@code @JsonDeserialize(using = StringDeserializer.class)}
 *                because the body is legitimate HTML from an authenticated user.
 */
public record SendLeadEmailRequest(

        @NotBlank(message = "Subject is required")
        @Size(max = 200, message = "Subject must not exceed 200 characters")
        String subject,

        @NotBlank(message = "Body is required")
        @Size(max = 10_000, message = "Body must not exceed 10 000 characters")
        @JsonDeserialize(using = StringDeserializer.class)
        String body

) {}
