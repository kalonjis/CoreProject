package be.steby.CoreProject.pl.domains.deal.models.requests;

import be.steby.CoreProject.bll.domains.crm.deal.models.DealFilterRequest;
import be.steby.CoreProject.dl.enums.crm.DealStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PL request model for filtering the deal list.
 *
 * <p>All fields are optional — passed as query parameters on
 * {@code GET /api/crm/deals}. {@code null} means no restriction
 * on that criterion.</p>
 *
 * @param keyword              search term matched against deal title
 * @param status               filter by lifecycle status
 * @param pipelinePublicId     filter deals belonging to a specific pipeline
 * @param stagePublicId        filter deals at a specific stage
 * @param assignedToPublicId   filter deals assigned to a specific commercial
 * @param contactPublicId      filter deals linked to a specific contact
 * @param organisationPublicId filter deals linked to a specific organisation
 * @param overdueOnly          if {@code true}, returns only overdue open deals
 * @param amountMin            minimum deal amount, inclusive
 * @param amountMax            maximum deal amount, inclusive
 * @param expectedCloseFrom    earliest expected close date, inclusive
 * @param expectedCloseTo      latest expected close date, inclusive
 * @param tagPublicId          filter deals that have a specific tag applied
 */
public record DealListFilterRequest(

        String keyword,
        DealStatus status,
        String pipelinePublicId,
        String stagePublicId,
        String assignedToPublicId,
        String contactPublicId,
        String organisationPublicId,
        Boolean overdueOnly,
        BigDecimal amountMin,
        BigDecimal amountMax,
        LocalDate expectedCloseFrom,
        LocalDate expectedCloseTo,
        String tagPublicId

) {

    /**
     * Converts this PL request to the BLL filter model.
     *
     * @return {@link DealFilterRequest} for the service layer
     */
    public DealFilterRequest toBllModel() {
        return new DealFilterRequest(
                keyword,
                status,
                pipelinePublicId,
                stagePublicId,
                assignedToPublicId,
                contactPublicId,
                organisationPublicId,
                overdueOnly,
                amountMin,
                amountMax,
                expectedCloseFrom,
                expectedCloseTo,
                tagPublicId
        );
    }
}
