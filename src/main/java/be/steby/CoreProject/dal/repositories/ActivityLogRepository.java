package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // =========================================================================
    // User — full history (paginated)
    // =========================================================================

    /**
     * All logs for a given user, newest first.
     * Covers every action category.
     */
    Page<ActivityLog> findByUserOrderByTimestampDesc(User user, Pageable pageable);

    /**
     * All logs for a given user within a time window, newest first.
     */
    @Query("""
            SELECT al FROM ActivityLog al
            WHERE al.user = :user
              AND al.timestamp BETWEEN :from AND :to
            ORDER BY al.timestamp DESC
            """)
    Page<ActivityLog> findByUserAndTimestampBetween(
            @Param("user") User user,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    // =========================================================================
    // User — filtered by category (paginated)
    // =========================================================================

    /**
     * Logs for a given user filtered by action category (e.g. "AUTH", "SECURITY"),
     * newest first.
     *
     * <p>Category values match {@code ActionLogType.getCategory()} — uppercase strings
     * stored in the {@code action_category} column.</p>
     */
    Page<ActivityLog> findByUserAndActionCategoryOrderByTimestampDesc(
            User user, String actionCategory, Pageable pageable);

    /**
     * Logs for a given user filtered by category and time window, newest first.
     */
    @Query("""
            SELECT al FROM ActivityLog al
            WHERE al.user = :user
              AND al.actionCategory = :category
              AND al.timestamp BETWEEN :from AND :to
            ORDER BY al.timestamp DESC
            """)
    Page<ActivityLog> findByUserAndActionCategoryAndTimestampBetween(
            @Param("user") User user,
            @Param("category") String category,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    // =========================================================================
    // User — filtered by action type (paginated)
    // =========================================================================

    /**
     * Logs for a given user filtered by a specific action type (e.g. "LOGIN",
     * "LOGIN_FAILED"), newest first.
     *
     * <p>Action type values match {@code ActionLogType.getName()} — the enum
     * constant name stored in the {@code action_type} column.</p>
     */
    Page<ActivityLog> findByUserAndActionTypeOrderByTimestampDesc(
            User user, String actionType, Pageable pageable);

    // =========================================================================
    // Admin — any user's logs
    // =========================================================================

    /**
     * All logs for a given user filtered by category and time window.
     * Intended for admin audit views where a broader time range is expected.
     *
     * <p>Same query as the user-facing variant but exposed separately to make
     * the intended caller explicit at the service layer.</p>
     */
    @Query("""
            SELECT al FROM ActivityLog al
            WHERE al.user = :user
              AND al.actionCategory = :category
              AND al.timestamp BETWEEN :from AND :to
            ORDER BY al.timestamp DESC
            """)
    Page<ActivityLog> findByUserAndCategoryBetweenForAdmin(
            @Param("user") User user,
            @Param("category") String category,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    // =========================================================================
    // GDPR — anonymise / delete
    // =========================================================================

    /**
     * Anonymises all logs belonging to a user by setting the user FK to null.
     * Preserves the log entries for audit continuity while removing the PII link.
     *
     * <p>Called during GDPR deletion ({@code gdprUserDelete}) so that security
     * and audit records are retained but can no longer be attributed to a
     * specific individual.</p>
     */
    @Modifying
    @Query("UPDATE ActivityLog al SET al.user = null WHERE al.user = :user")
    void anonymiseByUser(@Param("user") User user);

    /**
     * Hard-deletes all logs for a user.
     * Use only for permanent account deletion ({@code deleteUser}), not GDPR.
     */
    @Modifying
    @Query("DELETE FROM ActivityLog al WHERE al.user = :user")
    void deleteByUser(@Param("user") User user);
}