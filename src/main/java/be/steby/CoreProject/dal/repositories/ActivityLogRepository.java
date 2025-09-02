package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // ================== BASIC QUERIES ==================

    /**
     * Find all logs for a user ordered by timestamp desc
     */
    Page<ActivityLog> findByUserOrderByTimestampDesc(User user, Pageable pageable);

    /**
     * Find logs by user and category (domain)
     */
    Page<ActivityLog> findByUserAndActionCategoryOrderByTimestampDesc(
            User user, String actionCategory, Pageable pageable);

    /**
     * Find logs by user, category and timestamp after
     */
    Page<ActivityLog> findByUserAndActionCategoryAndTimestampAfterOrderByTimestampDesc(
            User user, String actionCategory, Instant after, Pageable pageable);

    /**
     * Find failed actions by user and category
     */
    Page<ActivityLog> findByUserAndActionCategoryAndSuccessfulOrderByTimestampDesc(
            User user, String actionCategory, boolean successful, Pageable pageable);

    /**
     * Find logs by device
     */
    Page<ActivityLog> findByDeviceOrderByTimestampDesc(Device device, Pageable pageable);

    /**
     * Find logs by user and device
     */
    Page<ActivityLog> findByUserAndDeviceOrderByTimestampDesc(
            User user, Device device, Pageable pageable);

    // ================== SPECIFIC ACTION TYPE QUERIES ==================

    /**
     * Find logs by specific action type
     */
    Page<ActivityLog> findByActionTypeOrderByTimestampDesc(String actionType, Pageable pageable);

    /**
     * Find logs by user and specific action type
     */
    Page<ActivityLog> findByUserAndActionTypeOrderByTimestampDesc(
            User user, String actionType, Pageable pageable);

    /**
     * Find recent logs of specific action type for user
     */
    List<ActivityLog> findTop10ByUserAndActionTypeOrderByTimestampDesc(
            User user, String actionType);

    // ================== COUNTING QUERIES ==================

    /**
     * Count actions of specific type for user after timestamp
     */
    long countByUserAndActionTypeAndTimestampAfter(
            User user, String actionType, Instant after);

    /**
     * Count successful/failed actions by category
     */
    long countByUserAndActionCategoryAndSuccessfulAndTimestampAfter(
            User user, String actionCategory, boolean successful, Instant after);

    // ================== EXISTENCE QUERIES ==================

    /**
     * Check if user performed specific action after timestamp (anti-spam)
     */
    boolean existsByUserAndActionTypeAndTimestampAfter(
            User user, String actionType, Instant after);

    /**
     * Check if user has any failed login attempts recently
     */
    boolean existsByUserAndActionTypeAndSuccessfulAndTimestampAfter(
            User user, String actionType, boolean successful, Instant after);

    // ================== COMPLEX QUERIES ==================

    /**
     * Find suspicious activities: same user, different devices in short time
     */
    @Query("SELECT al FROM ActivityLog al WHERE al.user = :user " +
            "AND al.actionCategory = 'AUTH' AND al.successful = true " +
            "AND al.timestamp BETWEEN :startTime AND :endTime " +
            "ORDER BY al.timestamp DESC")
    List<ActivityLog> findSuspiciousAuthActivities(
            @Param("user") User user,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Get login statistics by day
     */
    @Query(value = "SELECT DATE(timestamp) as login_date, COUNT(*) as login_count " +
            "FROM activity_log " +
            "WHERE action_type = 'LOGIN' AND successful = true " +
            "AND timestamp BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(timestamp) " +
            "ORDER BY login_date", nativeQuery = true)
    List<Object[]> countSuccessfulLoginsByDay(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find failed login attempts for security analysis
     */
    @Query("SELECT al FROM ActivityLog al WHERE al.actionType = 'LOGIN_FAILED' " +
            "AND al.timestamp > :since " +
            "ORDER BY al.timestamp DESC")
    List<ActivityLog> findRecentFailedLogins(@Param("since") Instant since);

    // ================== ADMIN QUERIES ==================

    /**
     * Search logs with multiple criteria (for admin interface)
     */
    @Query("SELECT al FROM ActivityLog al WHERE " +
            "(:userId IS NULL OR al.user.id = :userId) " +
            "AND (:deviceId IS NULL OR al.device.id = :deviceId) " +
            "AND (:actionCategory IS NULL OR al.actionCategory = :actionCategory) " +
            "AND (:successful IS NULL OR al.successful = :successful) " +
            "AND (:startDate IS NULL OR al.timestamp >= :startDate) " +
            "AND (:endDate IS NULL OR al.timestamp <= :endDate) " +
            "ORDER BY al.timestamp DESC")
    Page<ActivityLog> searchLogs(
            @Param("userId") Long userId,
            @Param("deviceId") Long deviceId,
            @Param("actionCategory") String actionCategory,
            @Param("successful") Boolean successful,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    // ================== CLEANUP QUERIES ==================

    /**
     * Delete old activity logs for maintenance
     */
    @Modifying
    @Query("DELETE FROM ActivityLog al WHERE al.timestamp < :cutoffDate")
    int deleteOldLogs(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Count logs older than cutoff date
     */
    long countByTimestampBefore(Instant cutoffDate);
}