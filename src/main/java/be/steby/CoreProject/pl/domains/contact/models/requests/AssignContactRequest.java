package be.steby.CoreProject.pl.domains.contact.models.requests;

import be.steby.CoreProject.bll.domains.crm.contact.models.ContactAssignRequest;

/**
 * PL request model for assigning or unassigning a commercial to a contact.
 *
 * <h3>Unassignment</h3>
 * <p>Passing {@code null} as {@code commercialPublicId} removes the current assignee.</p>
 *
 * @param commercialPublicId public UUID of the commercial to assign,
 *                           or {@code null} to unassign
 */
public record AssignContactRequest(
        String commercialPublicId
) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link ContactAssignRequest} for the service layer
     */
    public ContactAssignRequest toBllModel() {
        return new ContactAssignRequest(commercialPublicId);
    }
}
