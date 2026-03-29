package be.steby.CoreProject.pl.domains.organisation.models.requests;

import be.steby.CoreProject.bll.domains.organisation.models.OrganisationFilterRequest;
import be.steby.CoreProject.dl.enums.crm.OrganisationSize;

/**
 * PL request model for filtering the organisation list.
 *
 * <p>All fields are optional — passed as query parameters on
 * {@code GET /api/crm/organisations}. {@code null} means no restriction
 * on that criterion.</p>
 *
 * @param keyword     search term matched against the organisation name
 * @param industry    filter by sector of activity keyword
 * @param size        filter by headcount size bucket
 * @param countryCode ISO 3166-1 alpha-2 country code to filter by address location
 * @param tagPublicId public UUID of the tag to filter by (optional)
 */
public record OrganisationListFilterRequest(

        String keyword,
        String industry,
        OrganisationSize size,
        String countryCode,
        String tagPublicId

) {

    /**
     * Converts this PL request to the BLL filter model.
     *
     * @return {@link OrganisationFilterRequest} for the service layer
     */
    public OrganisationFilterRequest toBllModel() {
        return new OrganisationFilterRequest(keyword, industry, size, countryCode, tagPublicId);
    }
}
