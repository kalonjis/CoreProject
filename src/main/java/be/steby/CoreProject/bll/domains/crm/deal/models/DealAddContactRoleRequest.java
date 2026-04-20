package be.steby.CoreProject.bll.domains.crm.deal.models;

import be.steby.CoreProject.dl.enums.crm.ContactRole;

/**
 * BLL request for adding a contact to a deal with a specific role.
 *
 * @param contactPublicId public UUID of the contact to add
 * @param role            the contact's role on this deal (defaults to OTHER if null)
 */
public record DealAddContactRoleRequest(
        String contactPublicId,
        ContactRole role
) {}
