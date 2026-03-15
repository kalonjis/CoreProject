package be.steby.CoreProject.pl.domains.supportticket.models.responses;

import be.steby.CoreProject.dl.entities.crm.SupportTicket;
import be.steby.CoreProject.dl.enums.crm.SupportTicketStatus;

import java.time.Instant;

/**
 * Lightweight response model for support ticket list views.
 *
 * @param publicId             public UUID of the ticket
 * @param subject              short subject line
 * @param status               current lifecycle status
 * @param contactPublicId      public UUID of the submitting contact
 * @param contactFullName      full name of the submitting contact
 * @param assignedToPublicId   public UUID of the assigned team member, or {@code null}
 * @param assignedToUsername   username of the assigned team member, or {@code null}
 * @param createdAt            timestamp of ticket creation
 */
public record SupportTicketSummaryResponse(
        String publicId,
        String subject,
        SupportTicketStatus status,
        String contactPublicId,
        String contactFullName,
        String assignedToPublicId,
        String assignedToUsername,
        Instant createdAt
) {
    public static SupportTicketSummaryResponse fromEntity(SupportTicket ticket) {
        return new SupportTicketSummaryResponse(
                ticket.getPublicId(),
                ticket.getSubject(),
                ticket.getStatus(),
                ticket.getSubmittedBy() != null ? ticket.getSubmittedBy().getPublicId()   : null,
                ticket.getSubmittedBy() != null ? ticket.getSubmittedBy().getFullName()   : null,
                ticket.getAssignedTo()  != null ? ticket.getAssignedTo().getPublicId()    : null,
                ticket.getAssignedTo()  != null ? ticket.getAssignedTo().getUsername()    : null,
                ticket.getCreatedAt()
        );
    }
}
