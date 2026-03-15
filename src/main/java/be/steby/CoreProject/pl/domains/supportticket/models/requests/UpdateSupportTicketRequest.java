package be.steby.CoreProject.pl.domains.supportticket.models.requests;

import be.steby.CoreProject.bll.domains.supportticket.models.SupportTicketUpdateRequest;
import jakarta.validation.constraints.Size;

/**
 * PL request model for partially updating a support ticket's content fields.
 *
 * @param subject     new subject line (optional)
 * @param description new description (optional)
 */
public record UpdateSupportTicketRequest(

        @Size(max = 255, message = "Subject must not exceed 255 characters")
        String subject,

        String description

) {
    public SupportTicketUpdateRequest toBllModel() {
        return new SupportTicketUpdateRequest(
                subject != null ? subject.trim() : null,
                description != null ? description.trim() : null
        );
    }
}
