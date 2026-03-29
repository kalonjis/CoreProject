package be.steby.CoreProject.dal.specifications.crm;

import be.steby.CoreProject.bll.common.models.changelog.CrmChangeLogFilter;
import be.steby.CoreProject.dl.entities.crm.CrmChangeLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/**
 * JPA {@link Specification} builders for {@link CrmChangeLog} queries.
 *
 * <p>All predicates are null-safe — a null value in the filter simply omits
 * that predicate, making every field optional without combinatorial repository
 * method explosion.</p>
 */
public final class CrmChangeLogSpecification {

    private CrmChangeLogSpecification() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /**
     * Builds a composed specification from all non-null fields in the given filter.
     *
     * @param filter the BLL filter containing optional constraints
     * @return a composed {@link Specification} ready for {@code repository.findAll()}
     */
    public static Specification<CrmChangeLog> build(CrmChangeLogFilter filter) {
        return Specification
                .where(forEntity(filter))
                .and(fromDate(filter.from()))
                .and(toDate(filter.to()));
    }

    // ─── Private predicates ───────────────────────────────────────────────────

    /**
     * Restricts results to a specific entity (type + publicId).
     * Both fields must be non-null for the predicate to apply.
     */
    private static Specification<CrmChangeLog> forEntity(CrmChangeLogFilter filter) {
        return (root, query, cb) -> {
            if (filter.entityType() == null || filter.entityPublicId() == null) return null;
            return cb.and(
                    cb.equal(root.get("entityType"), filter.entityType()),
                    cb.equal(root.get("entityPublicId"), filter.entityPublicId())
            );
        };
    }

    /**
     * Restricts results to entries at or after the given timestamp.
     *
     * @param from lower bound, inclusive; null means no lower bound
     */
    private static Specification<CrmChangeLog> fromDate(Instant from) {
        return (root, query, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.get("changedAt"), from);
    }

    /**
     * Restricts results to entries at or before the given timestamp.
     *
     * @param to upper bound, inclusive; null means no upper bound
     */
    private static Specification<CrmChangeLog> toDate(Instant to) {
        return (root, query, cb) ->
                to == null ? null : cb.lessThanOrEqualTo(root.get("changedAt"), to);
    }
}
