package be.steby.CoreProject.pl.domains.deal.models.responses;

import be.steby.CoreProject.dl.entities.crm.DealContactRole;
import be.steby.CoreProject.dl.enums.crm.ContactRole;

/**
 * Response model for a contact's role on a deal.
 *
 * @param contactPublicId the public UUID of the contact
 * @param contactFullName the full name of the contact
 * @param contactEmail    the email of the contact
 * @param role            the contact's role on this deal
 * @param primary         whether this is the primary contact
 */
public record DealContactRoleResponse(
        String contactPublicId,
        String contactFullName,
        String contactEmail,
        ContactRole role,
        boolean primary
) {

    public static DealContactRoleResponse fromEntity(DealContactRole dcr) {
        return new DealContactRoleResponse(
                dcr.getContact().getPublicId(),
                dcr.getContact().getFullName(),
                dcr.getContact().getEmail(),
                dcr.getRole(),
                dcr.isPrimary()
        );
    }
}
