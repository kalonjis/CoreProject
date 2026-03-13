package be.steby.CoreProject.pl.domains.organisation.models.requests;

import be.steby.CoreProject.bll.domains.organisation.models.OrganisationCreateRequest;
import be.steby.CoreProject.dl.enums.crm.OrganisationSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PL request model for creating a new organisation.
 *
 * @param name            legal or commercial name of the organisation (required)
 * @param website         public website URL (optional)
 * @param industry        sector of activity, free-text (optional)
 * @param size            approximate headcount bucket (optional)
 * @param phone           main switchboard or reception phone number (optional)
 * @param addressPublicId public UUID of an existing address to link (optional)
 * @param notes           internal notes for the commercial team (optional)
 */
public record CreateOrganisationRequest(

        @NotBlank(message = "Organisation name is required")
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
     * @return {@link OrganisationCreateRequest} for the service layer
     */
    public OrganisationCreateRequest toBllModel() {
        return new OrganisationCreateRequest(
                name.trim(),
                website  != null ? website.trim()  : null,
                industry != null ? industry.trim() : null,
                size,
                phone    != null ? phone.trim()    : null,
                addressPublicId,
                notes    != null ? notes.trim()    : null
        );
    }
}
