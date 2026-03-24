package be.steby.CoreProject.dal.specifications.crm;

import be.steby.CoreProject.dl.entities.crm.Deal;
import be.steby.CoreProject.dl.entities.crm.DealContactRole;
import be.steby.CoreProject.dl.enums.crm.DealStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for dynamic filtering of {@link Deal} entities.
 *
 * <p>Each static method returns a {@link Specification} that can be combined
 * with others to build complex deal list and Kanban filters at runtime.</p>
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * Specification<Deal> spec = Specification
 *     .where(DealSpecification.hasStatus(DealStatus.OPEN))
 *     .and(DealSpecification.inPipeline(pipelineId))
 *     .and(DealSpecification.assignedTo(userId))
 *     .and(DealSpecification.amountBetween(new BigDecimal("500"), new BigDecimal("5000")))
 *     .and(DealSpecification.titleContains("nettoyage"));
 *
 * Page<Deal> results = dealRepository.findAll(spec, pageable);
 * }</pre>
 *
 * <h3>Null safety</h3>
 * <p>All methods return {@code null} when the filter value is {@code null},
 * which JPA treats as "no restriction" — safe to chain unconditionally.</p>
 */
public class DealSpecification {

    private DealSpecification() {
        // Utility class — not instantiable
    }

    // =========================================================================
    // Text search
    // =========================================================================

    /**
     * Filters deals whose title contains the given keyword (case-insensitive).
     *
     * <p>Used for the search bar in the deal list view.</p>
     *
     * @param keyword the search term, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Deal> titleContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase().trim() + "%";
            return cb.like(cb.lower(root.get("title")), pattern);
        };
    }

    // =========================================================================
    // Status
    // =========================================================================

    /**
     * Filters deals by their lifecycle status.
     *
     * @param status the status to filter by, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Deal> hasStatus(DealStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    // =========================================================================
    // Pipeline & stage
    // =========================================================================

    /**
     * Filters deals belonging to a specific pipeline.
     *
     * @param pipelineId the internal ID of the pipeline, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Deal> inPipeline(Long pipelineId) {
        if (pipelineId == null) return null;
        return (root, query, cb) ->
            cb.equal(root.get("pipeline").get("id"), pipelineId);
    }

    /**
     * Filters deals currently at a specific stage.
     *
     * @param stageId the internal ID of the stage, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Deal> atStage(Long stageId) {
        if (stageId == null) return null;
        return (root, query, cb) ->
            cb.equal(root.get("pipelineStep").get("id"), stageId);
    }

    // =========================================================================
    // Assignment
    // =========================================================================

    /**
     * Filters deals assigned to a specific commercial.
     *
     * @param assignedToId the internal ID of the commercial, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Deal> assignedTo(Long assignedToId) {
        if (assignedToId == null) return null;
        return (root, query, cb) ->
            cb.equal(root.get("assignedTo").get("id"), assignedToId);
    }

    // =========================================================================
    // Contact & organisation
    // =========================================================================

    /**
     * Filters deals linked to a specific contact.
     *
     * @param contactId the internal ID of the contact, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Deal> forContact(Long contactId) {
        if (contactId == null) return null;
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<DealContactRole> dcr = sub.from(DealContactRole.class);
            sub.select(dcr.get("deal").get("id"))
               .where(cb.equal(dcr.get("contact").get("id"), contactId));
            return root.get("id").in(sub);
        };
    }

    /**
     * Filters deals linked to a specific organisation.
     *
     * @param organisationId the internal ID of the organisation, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Deal> forOrganisation(Long organisationId) {
        if (organisationId == null) return null;
        return (root, query, cb) ->
            cb.equal(root.get("organisation").get("id"), organisationId);
    }

    // =========================================================================
    // Amount range
    // =========================================================================

    /**
     * Filters deals whose amount falls within an optional range.
     *
     * <p>Both bounds are optional — pass {@code null} to leave a bound open.</p>
     *
     * @param min minimum amount (inclusive), or {@code null}
     * @param max maximum amount (inclusive), or {@code null}
     * @return the specification, or {@code null} if both bounds are null
     */
    public static Specification<Deal> amountBetween(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) return null;
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (min != null) predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), min));
            if (max != null) predicates.add(cb.lessThanOrEqualTo(root.get("amount"), max));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // =========================================================================
    // Close date
    // =========================================================================

    /**
     * Filters deals whose expected close date falls within an optional range.
     *
     * <p>Both bounds are optional — pass {@code null} to leave a bound open.</p>
     *
     * @param from start date (inclusive), or {@code null}
     * @param to   end date (inclusive), or {@code null}
     * @return the specification, or {@code null} if both bounds are null
     */
    public static Specification<Deal> expectedCloseBetween(LocalDate from, LocalDate to) {
        if (from == null && to == null) return null;
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("expectedCloseDate"), from));
            if (to   != null) predicates.add(cb.lessThanOrEqualTo(root.get("expectedCloseDate"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filters deals that are overdue — open with an expected close date in the past.
     *
     * @return the specification
     */
    public static Specification<Deal> isOverdue() {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("status"), DealStatus.OPEN),
            cb.lessThan(root.get("expectedCloseDate"), LocalDate.now())
        );
    }
}