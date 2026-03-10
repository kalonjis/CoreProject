package be.steby.CoreProject.pl.domains.lead.models.responses;

import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.dl.enums.crm.LeadStatus;

import java.time.Instant;

/**
 * Lightweight response model for lead list and queue views.
 *
 * <p>Used in paginated lists — contains only the fields needed
 * to display a row in the admin lead queue. For full details,
 * see {@link LeadDetailResponse}.</p>
 *
 * @param publicId        public UUID of the lead
 * @param email           email address of the visitor
 * @param name            name of the visitor, or {@code null} if not provided
 * @param subject         subject of the inquiry
 * @param leadType        type of inquiry
 * @param status          current CRM processing status
 * @param assignedTo      username of the assigned commercial, or {@code null} if unassigned
 * @param submittedAt     timestamp of the original submission
 * @param convertedAt     timestamp of conversion, or {@code null} if not converted
 */
public record LeadSummaryResponse(
        String publicId,
        String email,
        String name,
        String subject,
        LeadType leadType,
        LeadStatus status,
        String assignedTo,
        Instant submittedAt,
        Instant convertedAt
) {

    /**
     * Maps a {@link Lead} entity to a {@link LeadSummaryResponse}.
     *
     * @param lead the lead entity
     * @return the summary response
     */
    public static LeadSummaryResponse fromEntity(Lead lead) {
        return new LeadSummaryResponse(
                lead.getPublicId(),
                lead.getEmail(),
                lead.getName(),
                lead.getSubject(),
                lead.getLeadType(),
                lead.getStatus(),
                lead.getAssignedTo() != null ? lead.getAssignedTo().getUsername() : null,
                lead.getSubmittedAt(),
                lead.getConvertedAt()
        );
    }
}