package be.steby.CoreProject.dal.specifications;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import be.steby.CoreProject.bll.common.models.activitylog.ActivityLogFilter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for {@link ActivityLog} dynamic queries.
 *
 * <p>Each static method returns a single-concern {@link Specification} predicate
 * that can be composed freely via {@link Specification#and} / {@link Specification#or}.
 * This avoids the combinatorial explosion of explicit repository methods when
 * filters are multi-dimensional (user, category, date range, success status).</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * Specification<ActivityLog> spec = Specification
 *     .where(ActivityLogSpecification.forUser(user))
 *     .and(ActivityLogSpecification.withCategory("AUTH"))
 *     .and(ActivityLogSpecification.between(from, to))
 *     .and(ActivityLogSpecification.withSuccess(true));
 *
 * repository.findAll(spec, pageable);
 * }</pre>
 *
 * <p>All predicates are null-safe: passing {@code null} returns a no-op
 * specification ({@code conjunction}) so callers can chain unconditionally.</p>
 */
public final class ActivityLogSpecification {

    private ActivityLogSpecification() {}

    // =========================================================================
    // Single predicates
    // =========================================================================

    /**
     * Filters logs belonging to a specific user.
     * Pass {@code null} to match logs from all users (including system events).
     */
    public static Specification<ActivityLog> forUser(User user) {
        if (user == null) return noOp();
        return (root, query, cb) -> cb.equal(root.get("user"), user);
    }

    /**
     * Filters logs by action category prefix (case-insensitive).
     *
     * <p>Uses {@code LIKE 'category%'} instead of exact match to support both
     * flat categories (e.g. {@code "AUTH"} → matches {@code "AUTH"} only) and
     * parent categories (e.g. {@code "ADMIN"} → matches {@code "ADMIN_USER"},
     * {@code "ADMIN_ROLE"}, {@code "ADMIN_SECURITY"}, etc.).</p>
     *
     * <p>This works because flat categories have no sub-categories in DB, so
     * {@code LIKE 'AUTH%'} matches the exact value {@code "AUTH"} only.
     * {@code LIKE 'ADMIN%'} matches all admin sub-categories transparently.</p>
     *
     * Pass {@code null} to match all categories.
     */
    public static Specification<ActivityLog> withCategory(String category) {
        if (category == null || category.isBlank()) return noOp();
        return (root, query, cb) ->
                cb.like(cb.upper(root.get("actionCategory")), category.toUpperCase() + "%");
    }

    /**
     * Filters logs by a specific action type (e.g. "LOGIN", "PASSWORD_CHANGED").
     * The comparison is case-insensitive.
     * Pass {@code null} to match all action types.
     */
    public static Specification<ActivityLog> withActionType(String actionType) {
        if (actionType == null || actionType.isBlank()) return noOp();
        return (root, query, cb) ->
                cb.equal(cb.upper(root.get("actionType")), actionType.toUpperCase());
    }

    /**
     * Filters logs to those occurring at or after {@code from}.
     * Pass {@code null} to apply no lower bound.
     */
    public static Specification<ActivityLog> from(Instant from) {
        if (from == null) return noOp();
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("timestamp"), from);
    }

    /**
     * Filters logs to those occurring at or before {@code to}.
     * Pass {@code null} to apply no upper bound.
     */
    public static Specification<ActivityLog> to(Instant to) {
        if (to == null) return noOp();
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("timestamp"), to);
    }

    /**
     * Filters logs by their success status.
     * Pass {@code null} to match both successful and failed entries.
     */
    public static Specification<ActivityLog> withSuccess(Boolean successful) {
        if (successful == null) return noOp();
        return (root, query, cb) -> cb.equal(root.get("successful"), successful);
    }

    // =========================================================================
    // Composite helpers
    // =========================================================================

    /**
     * Convenience composite: applies {@link #from(Instant)} and {@link #to(Instant)}
     * together. Either bound may be null independently.
     */
    public static Specification<ActivityLog> between(Instant from, Instant to) {
        return Specification.allOf(from(from), to(to));
    }

    /**
     * Builds a fully dynamic specification from an {@link ActivityLogFilter}.
     * Any null field in the filter is ignored — no predicate is added for it.
     *
     * <p>This is the single entry point used by {@code ActivityLogQueryService}
     * for all queries — user-scoped and global alike. The BLL composes the
     * filter; this method translates it to JPA predicates.</p>
     *
     * @param filter the BLL filter model; must not be null
     * @return composed specification ready for {@code repository.findAll(spec, pageable)}
     */
    public static Specification<ActivityLog> build(ActivityLogFilter filter) {
        return Specification.allOf(
                forUser(filter.user()),
                withCategory(filter.category()),
                between(filter.from(), filter.to()),
                withSuccess(filter.successful()));
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Returns a no-op specification (always true) used as a null-safe fallback.
     */
    private static <T> Specification<T> noOp() {
        return (root, query, cb) -> cb.conjunction();
    }
}