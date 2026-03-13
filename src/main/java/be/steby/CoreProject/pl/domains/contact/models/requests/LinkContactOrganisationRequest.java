package be.steby.CoreProject.pl.domains.contact.models.requests;

import be.steby.CoreProject.bll.domains.contact.models.ContactLinkOrganisationRequest;

/**
 * PL request model for linking or unlinking an organisation to a contact.
 *
 * <h3>Unlinking</h3>
 * <p>Passing {@code null} as {@code organisationPublicId} removes the current
 * organisation link, making the contact independent (e.g., sole trader).</p>
 *
 * @param organisationPublicId public UUID of the organisation to link,
 *                             or {@code null} to unlink
 */
public record LinkContactOrganisationRequest(
        String organisationPublicId
) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link ContactLinkOrganisationRequest} for the service layer
     */
    public ContactLinkOrganisationRequest toBllModel() {
        return new ContactLinkOrganisationRequest(organisationPublicId);
    }
}
