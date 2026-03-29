package be.steby.CoreProject.pl.domains.contact.models.requests;

import be.steby.CoreProject.bll.domains.outreach.models.CrmOutreachRequest;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PL request model for sending a CRM outreach email to a contact.
 *
 * <p>Passed as the JSON body of {@code POST /api/crm/contacts/:publicId/email}.
 * The {@code contactPublicId} is taken from the URL path parameter, not the body.</p>
 *
 * @param subject      email subject written by the commercial (required, max 200 chars)
 * @param body         rich-text HTML produced by TipTap on the frontend (required, max 10 000 chars).
 *                     The global {@code XssProtectionConfig} deserializer is bypassed for this field
 *                     via {@code @JsonDeserialize(using = StringDeserializer.class)} because the body
 *                     is legitimate HTML. Dangerous patterns (javascript:, on*=) are not reachable
 *                     through TipTap's extension whitelist, and the sender is always authenticated.
 * @param dealPublicId public UUID of the deal to attach the interaction log to (optional)
 */
public record SendContactEmailRequest(

        @NotBlank(message = "Subject is required")
        @Size(max = 200, message = "Subject must not exceed 200 characters")
        String subject,

        @NotBlank(message = "Body is required")
        @Size(max = 10_000, message = "Body must not exceed 10 000 characters")
        @JsonDeserialize(using = StringDeserializer.class)
        String body,

        String dealPublicId

) {
    /**
     * Converts this PL request to the BLL outreach model.
     *
     * @param contactPublicId the public UUID of the target contact (from URL path)
     * @return {@link CrmOutreachRequest} for the service layer
     */
    public CrmOutreachRequest toBllModel(String contactPublicId) {
        return new CrmOutreachRequest(contactPublicId, dealPublicId, subject, body);
    }
}
