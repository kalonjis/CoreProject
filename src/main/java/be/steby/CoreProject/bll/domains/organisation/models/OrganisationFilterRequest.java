package be.steby.CoreProject.bll.domains.organisation.models;

import be.steby.CoreProject.dl.enums.crm.OrganisationSize;
import be.steby.CoreProject.dl.enums.crm.OrganisationStatus;

/**
 * BLL request model for filtering {@link be.steby.CoreProject.dl.entities.crm.Organisation}
 * entities in paginated list queries.
 *
 * <p>All fields are optional. The service layer passes this request to
 * {@link be.steby.CoreProject.dal.specifications.crm.OrganisationSpecification}
 * to build a dynamic {@code Specification} — {@code null} fields are ignored.</p>
 *
 * <h3>Mapping to specifications</h3>
 * <ul>
 *   <li>{@code keyword}     → {@code OrganisationSpecification.nameContains}</li>
 *   <li>{@code industry}    → {@code OrganisationSpecification.hasIndustry}</li>
 *   <li>{@code size}        → {@code OrganisationSpecification.hasSize}</li>
 *   <li>{@code status}      → {@code OrganisationSpecification.hasStatus}</li>
 *   <li>{@code countryCode} → {@code OrganisationSpecification.inCountry}</li>
 *   <li>{@code tagPublicId} → {@code OrganisationSpecification.hasTag}</li>
 * </ul>
 *
 * @param keyword     search term matched against the organisation name (optional)
 * @param industry    filter by sector of activity keyword (optional)
 * @param size        filter by headcount size bucket (optional)
 * @param status      filter by lifecycle status (optional)
 * @param countryCode ISO 3166-1 alpha-2 country code to filter by address location (optional)
 * @param tagPublicId public UUID of the tag to filter by (optional)
 */
public record OrganisationFilterRequest(
        String keyword,
        String industry,
        OrganisationSize size,
        OrganisationStatus status,
        String countryCode,
        String tagPublicId
) {}
