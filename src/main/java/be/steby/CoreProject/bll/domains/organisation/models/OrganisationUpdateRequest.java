package be.steby.CoreProject.bll.domains.organisation.models;

import be.steby.CoreProject.dl.enums.crm.OrganisationSize;

/**
 * BLL request model for updating an existing {@link be.steby.CoreProject.dl.entities.crm.Organisation}.
 *
 * <h3>Partial update</h3>
 * <p>All fields are optional. The service layer only updates fields
 * that are non-null in this request, leaving the rest unchanged.</p>
 *
 * <h3>Address</h3>
 * <p>Address linking is included here as a convenience for simple updates.
 * Passing {@code null} leaves the current address unchanged.
 * Full geocoding workflows should go through the dedicated address service.</p>
 *
 * @param name            legal or commercial name of the organisation (optional)
 * @param website         public website URL (optional)
 * @param industry        sector of activity, free-text (optional)
 * @param size            approximate headcount bucket (optional)
 * @param phone           main switchboard or reception phone number (optional)
 * @param addressPublicId public UUID of an existing address to link (optional)
 * @param notes           internal notes for the commercial team (optional)
 */
public record OrganisationUpdateRequest(
        String name,
        String website,
        String industry,
        OrganisationSize size,
        String phone,
        String addressPublicId,
        String notes
) {}
