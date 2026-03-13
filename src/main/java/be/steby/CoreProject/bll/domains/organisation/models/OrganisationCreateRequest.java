package be.steby.CoreProject.bll.domains.organisation.models;

import be.steby.CoreProject.dl.enums.crm.OrganisationSize;

/**
 * BLL request model for creating a new {@link be.steby.CoreProject.dl.entities.crm.Organisation}.
 *
 * <p>Used when a commercial registers a company directly in the CRM,
 * independently of any lead or contact creation flow.</p>
 *
 * <h3>Address</h3>
 * <p>{@code addressPublicId} is optional — not all organisations have a known
 * address at creation time. It can be linked via a dedicated update operation later.</p>
 *
 * <h3>Deduplication</h3>
 * <p>The service layer checks {@code name} for case-insensitive uniqueness before
 * persisting, to avoid duplicate organisation entries in the CRM.</p>
 *
 * @param name            legal or commercial name of the organisation (required)
 * @param website         public website URL (optional)
 * @param industry        sector of activity, free-text (optional)
 * @param size            approximate headcount bucket (optional)
 * @param phone           main switchboard or reception phone number (optional)
 * @param addressPublicId public UUID of an existing address to link (optional)
 * @param notes           internal notes for the commercial team (optional)
 */
public record OrganisationCreateRequest(
        String name,
        String website,
        String industry,
        OrganisationSize size,
        String phone,
        String addressPublicId,
        String notes
) {}
