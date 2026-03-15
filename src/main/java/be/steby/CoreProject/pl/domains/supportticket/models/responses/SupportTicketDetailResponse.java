package be.steby.CoreProject.pl.domains.supportticket.models.responses;

import be.steby.CoreProject.dl.entities.crm.SupportTicket;
import be.steby.CoreProject.dl.enums.crm.SupportTicketStatus;

import java.time.Instant;

/**
 * Full response model for the support ticket detail view.
 *
 * @param publicId             public UUID of the ticket
 * @param subject              short subject line
 * @param description          detailed description, or {@code null}
 * @param status               current lifecycle status
 * @param contactPublicId      public UUID of the submitting contact
 * @param contactFullName      full name of the submitting contact
 * @param assignedToPublicId   public UUID of the assigned team member, or {@code null}
 * @param assignedToUsername   username of the assigned team member, or {@code null}
 * @param createdAt            timestamp of ticket creation
 * @param updatedAt            timestamp of last update
 */
public record SupportTicketDetailResponse(
        String publicId,
        String subject,
        String description,
        SupportTicketStatus status,
        String contactPublicId,
        String contactFullName,
        String assignedToPublicId,
        String assignedToUsername,
        Instant createdAt,
        Instant updatedAt
) {
    public static SupportTicketDetailResponse fromEntity(SupportTicket ticket) {
        return new SupportTicketDetailResponse(
                ticket.getPublicId(),
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getSubmittedBy() != null ? ticket.getSubmittedBy().getPublicId()   : null,
                ticket.getSubmittedBy() != null ? ticket.getSubmittedBy().getFullName()   : null,
                ticket.getAssignedTo()  != null ? ticket.getAssignedTo().getPublicId()    : null,
                ticket.getAssignedTo()  != null ? ticket.getAssignedTo().getUsername()    : null,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
