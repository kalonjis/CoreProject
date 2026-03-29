package be.steby.CoreProject.bll.domains.deal.models;

import be.steby.CoreProject.dl.enums.crm.DealStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * BLL request model for filtering {@link be.steby.CoreProject.dl.entities.crm.Deal}
 * entities in paginated list queries.
 *
 * <p>All fields are optional. The service layer passes this request to
 * {@link be.steby.CoreProject.dal.specifications.crm.DealSpecification}
 * to build a dynamic {@code Specification} — {@code null} fields are ignored.</p>
 *
 * <h3>Mapping to specifications</h3>
 * <ul>
 *   <li>{@code keyword}               → {@code DealSpecification.titleContains}</li>
 *   <li>{@code status}                → {@code DealSpecification.hasStatus}</li>
 *   <li>{@code pipelinePublicId}      → resolved to internal ID → {@code DealSpecification.inPipeline}</li>
 *   <li>{@code stagePublicId}         → resolved to internal ID → {@code DealSpecification.atStage}</li>
 *   <li>{@code assignedToPublicId}    → resolved to internal ID → {@code DealSpecification.assignedTo}</li>
 *   <li>{@code contactPublicId}       → resolved to internal ID → {@code DealSpecification.forContact}</li>
 *   <li>{@code organisationPublicId}  → resolved to internal ID → {@code DealSpecification.forOrganisation}</li>
 *   <li>{@code overdueOnly}           → {@code DealSpecification.isOverdue} (takes precedence over date filters)</li>
 *   <li>{@code amountMin} / {@code amountMax} → {@code DealSpecification.amountBetween}</li>
 *   <li>{@code expectedCloseFrom} / {@code expectedCloseTo} → {@code DealSpecification.expectedCloseBetween}</li>
 *   <li>{@code tagPublicId}           → resolved to internal ID → {@code DealSpecification.hasTag}</li>
 * </ul>
 *
 * @param keyword              search term matched against deal title (optional)
 * @param status               filter by lifecycle status (optional)
 * @param pipelinePublicId     filter deals belonging to a specific pipeline (optional)
 * @param stagePublicId        filter deals at a specific stage (optional)
 * @param assignedToPublicId   filter deals assigned to a specific commercial (optional)
 * @param contactPublicId      filter deals linked to a specific contact (optional)
 * @param organisationPublicId filter deals linked to a specific organisation (optional)
 * @param overdueOnly          if {@code true}, returns only overdue open deals (optional)
 * @param amountMin            minimum deal amount, inclusive (optional)
 * @param amountMax            maximum deal amount, inclusive (optional)
 * @param expectedCloseFrom    earliest expected close date, inclusive (optional)
 * @param expectedCloseTo      latest expected close date, inclusive (optional)
 * @param tagPublicId          filter deals that have a specific tag applied (optional)
 */
public record DealFilterRequest(
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
) {}
