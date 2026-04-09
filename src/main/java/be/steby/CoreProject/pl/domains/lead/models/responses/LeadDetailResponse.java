package be.steby.CoreProject.pl.domains.lead.models.responses;

import be.steby.CoreProject.bll.domains.lead.models.LeadDetailModel;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.dl.enums.crm.Civility;
import be.steby.CoreProject.dl.enums.crm.LeadSource;
import be.steby.CoreProject.dl.enums.crm.LeadStatus;

import java.time.Instant;
import java.util.Optional;

/**
 * Full response model for the lead detail view.
 *
 * <p>Includes all fields available on a lead, including enrichment data
 * (firstName, lastName, phone, organisationName) and CRM lifecycle data
 * (assignment, rejection reason, conversion timestamp).
 * For list views, use {@link LeadSummaryResponse} instead.</p>
 *
 * @param publicId           public UUID of the lead
 * @param email              email address of the visitor
 * @param civility           salutation of the visitor, or {@code null}
 * @param firstName          first name, or {@code null}
 * @param lastName           enriched last name, or {@code null}
 * @param phone              phone number, or {@code null}
 * @param organisationName   name of the organisation, or {@code null}
 * @param subject            subject of the inquiry
 * @param message            message body of the inquiry, or {@code null} for old leads
 * @param leadType           type of inquiry
 * @param leadSource         acquisition source, or {@code null}
 * @param status             current CRM processing status
 * @param assignedToPublicId public UUID of the assigned commercial, or {@code null}
 * @param assignedToUsername username of the assigned commercial, or {@code null}
 * @param rejectionReason    reason for rejection, or {@code null} if not rejected
 * @param submittedAt        timestamp of the original submission
 * @param convertedAt        timestamp of conversion, or {@code null} if not converted
 * @param createdAt                timestamp of entity creation
 * @param updatedAt                timestamp of last update
 * @param existingContactPublicId  public UUID of a Contact already sharing this email, or {@code null}
 */
public record LeadDetailResponse(
        String publicId,
        String email,
        Civility civility,
        String firstName,
        String lastName,
        String phone,
        String organisationName,
        String subject,
        String message,
        LeadType leadType,
        LeadSource leadSource,
        LeadStatus status,
        String assignedToPublicId,
        String assignedToUsername,
        String rejectionReason,
        Instant submittedAt,
        Instant convertedAt,
        Instant createdAt,
        Instant updatedAt,
        String existingContactPublicId
) {

    /**
     * Maps a {@link Lead} entity to a {@link LeadDetailResponse}.
     *
     * <p>The {@code assignedTo} user is extracted once to avoid multiple
     * calls on a potentially lazy-loaded association.</p>
     *
     * @param lead the lead entity
     * @return the detail response
     */
    public static LeadDetailResponse fromEntity(Lead lead) {
        User assignedTo = lead.getAssignedTo();
        return new LeadDetailResponse(
                lead.getPublicId(),
                lead.getEmail(),
                lead.getCivility(),
                lead.getFirstName(),
                lead.getLastName(),
                lead.getPhone(),
                lead.getOrganisationName(),
                lead.getSubject(),
                lead.getMessage(),
                lead.getLeadType(),
                lead.getLeadSource(),
                lead.getStatus(),
                Optional.ofNullable(assignedTo).map(User::getPublicId).orElse(null),
                Optional.ofNullable(assignedTo).map(User::getUsername).orElse(null),
                lead.getRejectionReason(),
                lead.getSubmittedAt(),
                lead.getConvertedAt(),
                lead.getCreatedAt(),
                lead.getUpdatedAt(),
                null
        );
    }

    /**
     * Maps a {@link LeadDetailModel} (lead + deduplication context) to a response.
     *
     * @param model the BLL detail model
     * @return the detail response including existingContactPublicId if applicable
     */
    public static LeadDetailResponse fromDetail(LeadDetailModel model) {
        Lead lead = model.lead();
        User assignedTo = lead.getAssignedTo();
        return new LeadDetailResponse(
                lead.getPublicId(),
                lead.getEmail(),
                lead.getCivility(),
                lead.getFirstName(),
                lead.getLastName(),
                lead.getPhone(),
                lead.getOrganisationName(),
                lead.getSubject(),
                lead.getMessage(),
                lead.getLeadType(),
                lead.getLeadSource(),
                lead.getStatus(),
                Optional.ofNullable(assignedTo).map(User::getPublicId).orElse(null),
                Optional.ofNullable(assignedTo).map(User::getUsername).orElse(null),
                lead.getRejectionReason(),
                lead.getSubmittedAt(),
                lead.getConvertedAt(),
                lead.getCreatedAt(),
                lead.getUpdatedAt(),
                model.existingContactPublicId()
        );
    }
}
