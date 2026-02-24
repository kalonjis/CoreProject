package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ActivityLogRepository
        extends JpaRepository<ActivityLog, Long>,
        JpaSpecificationExecutor<ActivityLog> {

    List<ActivityLog> findByUserOrderByTimestampDesc(User user);

    // =========================================================================
    // Stats / counts
    // =========================================================================

    /**
     * Total count of logs per action category across all users.
     * Each entry is an Object[] of [categoryString, Long count].
     * Used to build {@code ActivityLogStatsResponse}.
     */
    @Query("""
            SELECT al.actionCategory, COUNT(al)
            FROM ActivityLog al
            GROUP BY al.actionCategory
            """)
    List<Object[]> countByActionCategory();

    /**
     * Total count of logs per action category within a time window.
     * Allows stats to be scoped to a specific date range.
     */
    @Query("""
            SELECT al.actionCategory, COUNT(al)
            FROM ActivityLog al
            WHERE al.timestamp BETWEEN :from AND :to
            GROUP BY al.actionCategory
            """)
    List<Object[]> countByActionCategoryBetween(
            @Param("from") Instant from,
            @Param("to") Instant to);

    // =========================================================================
    // GDPR — anonymise / delete
    // =========================================================================

    /**
     * Anonymises all logs for a user by nullifying the user FK.
     * Preserves entries for audit continuity while removing the PII link.
     */
    @Modifying
    @Query("UPDATE ActivityLog al SET al.user = null WHERE al.user = :user")
    void anonymiseByUser(@Param("user") User user);

    /**
     * Hard-deletes all logs for a user.
     * Use only when full erasure is required (e.g. account deletion with
     * no audit retention policy).
     */
    @Modifying
    @Query("DELETE FROM ActivityLog al WHERE al.user = :user")
    void deleteByUser(@Param("user") User user);
}