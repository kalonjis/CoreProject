package be.steby.CoreProject.pl.domains.supportticket.models.requests;

import be.steby.CoreProject.bll.domains.supportticket.models.SupportTicketCreateRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PL request model for creating a new support ticket.
 *
 * @param subject             short subject line (required)
 * @param description         detailed description of the issue (optional)
 * @param submittedByPublicId public UUID of the contact submitting the ticket (required)
 * @param assignedToPublicId  public UUID of the user to assign immediately (optional)
 */
public record CreateSupportTicketRequest(

        @NotBlank(message = "Subject is required")
        @Size(max = 255, message = "Subject must not exceed 255 characters")
        String subject,

        String description,

        @NotBlank(message = "Contact is required")
        String submittedByPublicId,

        String assignedToPublicId

) {
    public SupportTicketCreateRequest toBllModel() {
        return new SupportTicketCreateRequest(
                subject.trim(),
                description != null ? description.trim() : null,
                submittedByPublicId,
                assignedToPublicId
        );
    }
}
