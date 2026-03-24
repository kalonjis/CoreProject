package be.steby.CoreProject.pl.domains.deal.models.requests;

import be.steby.CoreProject.bll.domains.deal.models.DealAddContactRoleRequest;
import be.steby.CoreProject.dl.enums.crm.ContactRole;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to add a contact to a deal with a specific role.
 *
 * @param contactPublicId public UUID of the contact to add
 * @param role            the contact's role on this deal (optional, defaults to OTHER)
 */
public record AddDealContactRoleRequest(
        @NotBlank String contactPublicId,
        ContactRole role
) {
    public DealAddContactRoleRequest toBllModel() {
        return new DealAddContactRoleRequest(contactPublicId, role);
    }
}
