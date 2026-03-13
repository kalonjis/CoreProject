package be.steby.CoreProject.pl.domains.contact.models.requests;

import be.steby.CoreProject.bll.domains.contact.models.ContactFilterRequest;
import be.steby.CoreProject.dl.enums.crm.ContactStatus;

/**
 * PL request model for filtering the contact list.
 *
 * <p>All fields are optional — passed as query parameters on
 * {@code GET /api/crm/contacts}. {@code null} means no restriction
 * on that criterion.</p>
 *
 * @param keyword              search term matched against first name, last name, and email
 * @param status               filter by CRM lifecycle status
 * @param organisationPublicId filter contacts belonging to a specific organisation
 * @param withoutOrganisation  if {@code true}, returns only independent contacts
 * @param assignedToPublicId   filter contacts assigned to a specific commercial
 * @param hasLinkedUser        if {@code true}, returns only contacts linked to a platform account
 * @param convertedFromLead    if {@code true}, returns only contacts converted from a lead
 */
public record ContactListFilterRequest(

        String keyword,
        ContactStatus status,
        String organisationPublicId,
        Boolean withoutOrganisation,
        String assignedToPublicId,
        Boolean hasLinkedUser,
        Boolean convertedFromLead

) {

    /**
     * Converts this PL request to the BLL filter model.
     *
     * @return {@link ContactFilterRequest} for the service layer
     */
    public ContactFilterRequest toBllModel() {
        return new ContactFilterRequest(
                keyword,
                status,
                organisationPublicId,
                withoutOrganisation,
                assignedToPublicId,
                hasLinkedUser,
                convertedFromLead
        );
    }
}
