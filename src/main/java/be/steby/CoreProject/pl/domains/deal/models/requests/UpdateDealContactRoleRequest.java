package be.steby.CoreProject.pl.domains.deal.models.requests;

import be.steby.CoreProject.dl.enums.crm.ContactRole;
import jakarta.validation.constraints.NotNull;

/**
 * Request to change a contact's role on a deal.
 *
 * @param role the new role to assign
 */
public record UpdateDealContactRoleRequest(
        @NotNull ContactRole role
) {}
