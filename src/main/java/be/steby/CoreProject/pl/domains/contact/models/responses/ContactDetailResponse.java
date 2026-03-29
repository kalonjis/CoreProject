package be.steby.CoreProject.pl.domains.contact.models.responses;

import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.enums.crm.ContactStatus;
import be.steby.CoreProject.pl.domains.tag.models.responses.TagResponse;

import java.time.Instant;
import java.util.List;

/**
 * Full response model for the contact detail view.
 *
 * <p>Includes all fields available on a contact, including CRM lifecycle
 * data (assignment, status, organisation, linked user, origin lead).
 * For list views, use {@link ContactSummaryResponse} instead.</p>
 *
 * @param publicId             public UUID of the contact
 * @param firstName            first name
 * @param lastName             last name
 * @param email                professional email address
 * @param phone                direct phone number, or {@code null} if not set
 * @param jobTitle             job title, or {@code null} if not set
 * @param status               current CRM lifecycle status
 * @param assignedToPublicId   public UUID of the assigned commercial, or {@code null}
 * @param assignedToUsername   username of the assigned commercial, or {@code null}
 * @param organisationPublicId public UUID of the linked organisation, or {@code null}
 * @param originLeadPublicId   public UUID of the originating lead, or {@code null}
 * @param hasLinkedUser        {@code true} if the contact has a linked platform account
 * @param linkedUserPublicId   public UUID of the linked platform account, or {@code null}
 * @param notes                internal notes, or {@code null} if none
 * @param createdAt            timestamp of entity creation
 * @param updatedAt            timestamp of last update
 */
public record ContactDetailResponse(
        String publicId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String jobTitle,
        ContactStatus status,
        String assignedToPublicId,
        String assignedToUsername,
        String organisationPublicId,
        String originLeadPublicId,
        boolean hasLinkedUser,
        String linkedUserPublicId,
        String notes,
        List<TagResponse> tags,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Maps a {@link Contact} entity to a {@link ContactDetailResponse}.
     *
     * @param contact the contact entity
     * @return the detail response
     */
    public static ContactDetailResponse fromEntity(Contact contact) {
        return new ContactDetailResponse(
                contact.getPublicId(),
                contact.getFirstName(),
                contact.getLastName(),
                contact.getEmail(),
                contact.getPhone(),
                contact.getJobTitle(),
                contact.getStatus(),
                contact.getAssignedTo()   != null ? contact.getAssignedTo().getPublicId()    : null,
                contact.getAssignedTo()   != null ? contact.getAssignedTo().getUsername()    : null,
                contact.getOrganisation() != null ? contact.getOrganisation().getPublicId()  : null,
                contact.getOriginLead()   != null ? contact.getOriginLead().getPublicId()    : null,
                contact.hasLinkedUser(),
                contact.getLinkedUser()   != null ? contact.getLinkedUser().getPublicId()    : null,
                contact.getNotes(),
                contact.getTags().stream().map(TagResponse::fromEntity).toList(),
                contact.getCreatedAt(),
                contact.getUpdatedAt()
        );
    }
}
