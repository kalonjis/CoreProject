package be.steby.CoreProject.pl.domains.organisation.models.requests;

import be.steby.CoreProject.bll.domains.organisation.models.OrganisationUpdateRequest;
import be.steby.CoreProject.dl.enums.crm.OrganisationSize;
import jakarta.validation.constraints.Size;

/**
 * PL request model for partially updating an existing organisation.
 *
 * <p>All fields are optional — only non-null fields are applied at the service layer.</p>
 *
 * @param name            legal or commercial name of the organisation (optional)
 * @param website         public website URL (optional)
 * @param industry        sector of activity, free-text (optional)
 * @param size            approximate headcount bucket (optional)
 * @param phone           main switchboard or reception phone number (optional)
 * @param addressPublicId public UUID of an existing address to link (optional)
 * @param notes           internal notes for the commercial team (optional)
 */
public record UpdateOrganisationRequest(

        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @Size(max = 255, message = "Website must not exceed 255 characters")
        String website,

        @Size(max = 100, message = "Industry must not exceed 100 characters")
        String industry,

        OrganisationSize size,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        String addressPublicId,

        String notes

) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link OrganisationUpdateRequest} for the service layer
     */
    public OrganisationUpdateRequest toBllModel() {
        return new OrganisationUpdateRequest(
                name     != null ? name.trim()     : null,
                website  != null ? website.trim()  : null,
                industry != null ? industry.trim() : null,
                size,
                phone    != null ? phone.trim()    : null,
                addressPublicId,
                notes    != null ? notes.trim()    : null
        );
    }
}
