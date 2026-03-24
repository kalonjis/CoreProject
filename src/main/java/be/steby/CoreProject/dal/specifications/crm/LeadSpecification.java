package be.steby.CoreProject.dal.specifications.crm;

import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.dl.enums.crm.LeadSource;
import be.steby.CoreProject.dl.enums.crm.LeadStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * JPA Specifications for dynamic filtering of {@link Lead} entities.
 *
 * <p>Each static method returns a {@link Specification} that can be combined
 * with others using {@code Specification.where().and().and()} to build
 * complex admin queue filters at runtime.</p>
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * Specification<Lead> spec = Specification
 *     .where(LeadSpecification.hasStatus(LeadStatus.NEW))
 *     .and(LeadSpecification.hasLeadType(LeadType.COMMERCIAL))
 *     .and(LeadSpecification.submittedBetween(from, to));
 *
 * Page<Lead> results = leadRepository.findAll(spec, pageable);
 * }</pre>
 *
 * <h3>Null safety</h3>
 * <p>All methods return {@code null} when the filter value is {@code null},
 * which JPA treats as "no restriction" — safe to chain unconditionally.</p>
 */
public class LeadSpecification {

    private LeadSpecification() {
        // Utility class — not instantiable
    }

    // =========================================================================
    // Status & type
    // =========================================================================

    /**
     * Filters leads by their CRM processing status.
     *
     * @param status the status to filter by, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Lead> hasStatus(LeadStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Filters leads by their inquiry type.
     *
     * @param leadType the type to filter by, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Lead> hasLeadType(LeadType leadType) {
        if (leadType == null) return null;
        return (root, query, cb) -> cb.equal(root.get("leadType"), leadType);
    }

    /**
     * Filters leads by their acquisition source.
     *
     * @param leadSource the source to filter by, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Lead> hasLeadSource(LeadSource leadSource) {
        if (leadSource == null) return null;
        return (root, query, cb) -> cb.equal(root.get("leadSource"), leadSource);
    }

    // =========================================================================
    // Assignment
    // =========================================================================

    /**
     * Filters leads assigned to a specific commercial.
     *
     * @param assignedToId the internal ID of the commercial, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Lead> assignedTo(Long assignedToId) {
        if (assignedToId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("assignedTo").get("id"), assignedToId);
    }

    /**
     * Filters leads that have not yet been assigned to any commercial.
     *
     * @return the specification
     */
    public static Specification<Lead> unassigned() {
        return (root, query, cb) -> cb.isNull(root.get("assignedTo"));
    }

    // =========================================================================
    // Date range
    // =========================================================================

    /**
     * Filters leads submitted within a date range.
     *
     * <p>Both bounds are optional — pass {@code null} to leave a bound open.</p>
     *
     * @param from start of the range (inclusive), or {@code null}
     * @param to   end of the range (inclusive), or {@code null}
     * @return the specification, or {@code null} if both bounds are null
     */
    public static Specification<Lead> submittedBetween(Instant from, Instant to) {
        if (from == null && to == null) return null;
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("submittedAt"), from));
            if (to   != null) predicates.add(cb.lessThanOrEqualTo(root.get("submittedAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // =========================================================================
    // Active filter
    // =========================================================================

    /**
     * Filters to only active (non-terminal) leads: {@code NEW} and {@code IN_REVIEW}.
     *
     * <p>Used as the default view — converted leads are in the Contact list,
     * and rejected leads are archived.</p>
     *
     * @param activeOnly if {@code true}, applies the filter; if {@code false} or {@code null}, skipped
     * @return the specification, or {@code null}
     */
    public static Specification<Lead> isActive(Boolean activeOnly) {
        if (!Boolean.TRUE.equals(activeOnly)) return null;
        return (root, query, cb) ->
                root.get("status").in(Arrays.asList(LeadStatus.NEW, LeadStatus.IN_REVIEW));
    }

    // =========================================================================
    // Text search
    // =========================================================================

    /**
     * Filters leads whose email, name, firstName, lastName, or organisationName
     * contains the given keyword (case-insensitive).
     *
     * <p>Used for the search bar in the admin lead queue.</p>
     *
     * @param keyword the search term, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Lead> emailOrNameContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("email")),            pattern),
                cb.like(cb.lower(root.get("name")),             pattern),
                cb.like(cb.lower(root.get("firstName")),        pattern),
                cb.like(cb.lower(root.get("lastName")),         pattern),
                cb.like(cb.lower(root.get("organisationName")), pattern)
            );
        };
    }
}