package be.steby.CoreProject.pl.domains.organisation.models.requests;

import be.steby.CoreProject.dl.enums.crm.OrganisationStatus;
import jakarta.validation.constraints.NotNull;

/**
 * PL request model for manually updating an organisation's lifecycle status.
 *
 * @param status the target status (required)
 */
public record UpdateOrganisationStatusRequest(
        @NotNull(message = "Status is required")
        OrganisationStatus status
) {}
