package be.steby.CoreProject.pl.domains.contact.models.responses;

import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.enums.crm.ContactStatus;

/**
 * Lightweight response model for contact list views.
 *
 * <p>Used in paginated lists — contains only the fields needed
 * to display a row in the CRM contact list. For full details,
 * see {@link ContactDetailResponse}.</p>
 *
 * @param publicId             public UUID of the contact
 * @param firstName            first name
 * @param lastName             last name
 * @param email                professional email address
 * @param jobTitle             job title, or {@code null} if not set
 * @param status               current CRM lifecycle status
 * @param organisationPublicId public UUID of the linked organisation, or {@code null}
 * @param organisationName     name of the linked organisation, or {@code null}
 * @param assignedTo           username of the assigned commercial, or {@code null}
 * @param hasLinkedUser        {@code true} if the contact has a linked platform account
 */
public record ContactSummaryResponse(
        String publicId,
        String firstName,
        String lastName,
        String email,
        String jobTitle,
        ContactStatus status,
        String organisationPublicId,
        String organisationName,
        String assignedTo,
        boolean hasLinkedUser
) {

    /**
     * Maps a {@link Contact} entity to a {@link ContactSummaryResponse}.
     *
     * @param contact the contact entity
     * @return the summary response
     */
    public static ContactSummaryResponse fromEntity(Contact contact) {
        return new ContactSummaryResponse(
                contact.getPublicId(),
                contact.getFirstName(),
                contact.getLastName(),
                contact.getEmail(),
                contact.getJobTitle(),
                contact.getStatus(),
                contact.getOrganisation() != null ? contact.getOrganisation().getPublicId() : null,
                contact.getOrganisation() != null ? contact.getOrganisation().getName()     : null,
                contact.getAssignedTo()   != null ? contact.getAssignedTo().getUsername()   : null,
                contact.hasLinkedUser()
        );
    }
}
