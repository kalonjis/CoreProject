package be.steby.CoreProject.pl.domains.lead.models.responses;

import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.dl.enums.crm.LeadStatus;

import java.time.Instant;

/**
 * Full response model for the lead detail view.
 *
 * <p>Includes all fields available on a lead, including CRM lifecycle
 * data (assignment, rejection reason, conversion timestamp).
 * For list views, use {@link LeadSummaryResponse} instead.</p>
 *
 * @param publicId          public UUID of the lead
 * @param email             email address of the visitor
 * @param name              name of the visitor, or {@code null} if not provided
 * @param subject           subject of the inquiry
 * @param leadType          type of inquiry
 * @param status            current CRM processing status
 * @param assignedToPublicId public UUID of the assigned commercial, or {@code null}
 * @param assignedToUsername username of the assigned commercial, or {@code null}
 * @param rejectionReason   reason for rejection, or {@code null} if not rejected
 * @param submittedAt       timestamp of the original submission
 * @param convertedAt       timestamp of conversion, or {@code null} if not converted
 * @param createdAt         timestamp of entity creation
 * @param updatedAt         timestamp of last update
 */
public record LeadDetailResponse(
        String publicId,
        String email,
        String name,
        String subject,
        LeadType leadType,
        LeadStatus status,
        String assignedToPublicId,
        String assignedToUsername,
        String rejectionReason,
        Instant submittedAt,
        Instant convertedAt,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Maps a {@link Lead} entity to a {@link LeadDetailResponse}.
     *
     * @param lead the lead entity
     * @return the detail response
     */
    public static LeadDetailResponse fromEntity(Lead lead) {
        return new LeadDetailResponse(
                lead.getPublicId(),
                lead.getEmail(),
                lead.getName(),
                lead.getSubject(),
                lead.getLeadType(),
                lead.getStatus(),
                lead.getAssignedTo() != null ? lead.getAssignedTo().getPublicId() : null,
                lead.getAssignedTo() != null ? lead.getAssignedTo().getUsername() : null,
                lead.getRejectionReason(),
                lead.getSubmittedAt(),
                lead.getConvertedAt(),
                lead.getCreatedAt(),
                lead.getUpdatedAt()
        );
    }
}