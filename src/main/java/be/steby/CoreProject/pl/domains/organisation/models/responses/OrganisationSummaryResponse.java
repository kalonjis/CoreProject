package be.steby.CoreProject.pl.domains.organisation.models.responses;

import be.steby.CoreProject.dl.entities.crm.Organisation;
import be.steby.CoreProject.dl.enums.crm.OrganisationSize;

/**
 * Lightweight response model for organisation list views.
 *
 * <p>Used in paginated lists — contains only the fields needed
 * to display a row in the CRM organisation list. For full details,
 * see {@link OrganisationDetailResponse}.</p>
 *
 * @param publicId  public UUID of the organisation
 * @param name      legal or commercial name
 * @param industry  sector of activity, or {@code null} if not set
 * @param size      approximate headcount bucket, or {@code null} if not set
 * @param website   public website URL, or {@code null} if not set
 * @param phone     main switchboard phone number, or {@code null} if not set
 */
public record OrganisationSummaryResponse(
        String publicId,
        String name,
        String industry,
        OrganisationSize size,
        String website,
        String phone
) {

    /**
     * Maps an {@link Organisation} entity to an {@link OrganisationSummaryResponse}.
     *
     * @param organisation the organisation entity
     * @return the summary response
     */
    public static OrganisationSummaryResponse fromEntity(Organisation organisation) {
        return new OrganisationSummaryResponse(
                organisation.getPublicId(),
                organisation.getName(),
                organisation.getIndustry(),
                organisation.getSize(),
                organisation.getWebsite(),
                organisation.getPhone()
        );
    }
}
