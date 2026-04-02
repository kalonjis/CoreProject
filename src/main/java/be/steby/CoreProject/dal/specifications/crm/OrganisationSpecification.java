package be.steby.CoreProject.dal.specifications.crm;

import be.steby.CoreProject.dl.entities.crm.Organisation;
import be.steby.CoreProject.dl.entities.crm.Tag;
import be.steby.CoreProject.dl.enums.crm.OrganisationSize;
import be.steby.CoreProject.dl.enums.crm.OrganisationStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for dynamic filtering of {@link Organisation} entities.
 *
 * <p>Each static method returns a {@link Specification} that can be combined
 * with others to build complex admin list filters at runtime.</p>
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * Specification<Organisation> spec = Specification
 *     .where(OrganisationSpecification.nameContains("acme"))
 *     .and(OrganisationSpecification.hasIndustry("Cleaning"))
 *     .and(OrganisationSpecification.hasSize(OrganisationSize.SMALL));
 *
 * Page<Organisation> results = organisationRepository.findAll(spec, pageable);
 * }</pre>
 *
 * <h3>Null safety</h3>
 * <p>All methods return {@code null} when the filter value is {@code null},
 * which JPA treats as "no restriction" — safe to chain unconditionally.</p>
 */
public class OrganisationSpecification {

    private OrganisationSpecification() {
        // Utility class — not instantiable
    }

    // =========================================================================
    // Text search
    // =========================================================================

    /**
     * Filters organisations whose name contains the given keyword (case-insensitive).
     *
     * <p>Used for the search bar and autocomplete in deal/contact creation forms.</p>
     *
     * @param keyword the search term, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Organisation> nameContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase().trim() + "%";
            return cb.like(cb.lower(root.get("name")), pattern);
        };
    }

    /**
     * Filters organisations whose industry contains the given keyword (case-insensitive).
     *
     * @param industry the industry keyword, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Organisation> hasIndustry(String industry) {
        if (industry == null || industry.isBlank()) return null;
        return (root, query, cb) -> {
            String pattern = "%" + industry.toLowerCase().trim() + "%";
            return cb.like(cb.lower(root.get("industry")), pattern);
        };
    }

    // =========================================================================
    // Classification
    // =========================================================================

    /**
     * Filters organisations by their headcount size bucket.
     *
     * @param size the size to filter by, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Organisation> hasSize(OrganisationSize size) {
        if (size == null) return null;
        return (root, query, cb) -> cb.equal(root.get("size"), size);
    }

    /**
     * Filters organisations by their lifecycle status.
     *
     * @param status the status to filter by, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Organisation> hasStatus(OrganisationStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    // =========================================================================
    // Address
    // =========================================================================

    /**
     * Filters organisations located in a specific country.
     *
     * <p>Joins to the {@code address} association to filter on {@code countryCode}.</p>
     *
     * @param countryCode the ISO 3166-1 alpha-2 country code (e.g. "BE"), or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Organisation> inCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) return null;
        return (root, query, cb) ->
            cb.equal(
                cb.lower(root.join("address").get("countryCode")),
                countryCode.toLowerCase().trim()
            );
    }

    // =========================================================================
    // Tags
    // =========================================================================

    /**
     * Filters organisations that have a specific tag applied.
     *
     * <p>Uses an INNER JOIN on the {@code tags} collection. {@code query.distinct(true)}
     * is applied to prevent duplicate rows when an organisation has multiple tags.</p>
     *
     * @param tagId the internal ID of the tag, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Organisation> hasTag(Long tagId) {
        if (tagId == null) return null;
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Organisation, Tag> tags = root.join("tags", JoinType.INNER);
            return cb.equal(tags.get("id"), tagId);
        };
    }
}