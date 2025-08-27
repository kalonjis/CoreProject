package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.actionLogTypes.ActionLogType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * ActivityLog Repository - Simple DAL with basic types
 * Pagination will be handled in services (BLL)
 */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // ================== TYPE-SAFE METHODS (NEW - Use ActionLogType enums) ==================

    /**
     * Find logs by ActionLogType (type-safe)
     */
    @Query("SELECT al FROM ActivityLog al WHERE " +
            "al.actionType = :#{#actionType.name()} AND " +
            "al.actionCategory = :#{#actionType.category} " +
            "ORDER BY al.timestamp DESC")
    List<ActivityLog> findByActionTypeOrderByTimestampDesc(@Param("actionType") ActionLogType actionType);

    /**
     * Find logs by user and ActionLogType (type-safe)
     */
    @Query("SELECT al FROM ActivityLog al WHERE " +
            "al.user = :user AND " +
            "al.actionType = :#{#actionType.name()} AND " +
            "al.actionCategory = :#{#actionType.category} " +
            "ORDER BY al.timestamp DESC")
    List<ActivityLog> findByUserAndActionTypeOrderByTimestampDesc(
            @Param("user") User user,
            @Param("actionType") ActionLogType actionType);

    /**
     * Find latest log by user and ActionLogType (type-safe)
     */
    @Query("SELECT al FROM ActivityLog al WHERE " +
            "al.user = :user AND " +
            "al.actionType = :#{#actionType.name()} AND " +
            "al.actionCategory = :#{#actionType.category} " +
            "ORDER BY al.timestamp DESC LIMIT 1")
    Optional<ActivityLog> findTopByUserAndActionTypeOrderByTimestampDesc(
            @Param("user") User user,
            @Param("actionType") ActionLogType actionType);

    /**
     * Find latest log by user and ActionLogType with success status (type-safe)
     */
    @Query("SELECT al FROM ActivityLog al WHERE " +
            "al.user = :user AND " +
            "al.actionType = :#{#actionType.name()} AND " +
            "al.actionCategory = :#{#actionType.category} AND " +
            "al.successful = :successful " +
            "ORDER BY al.timestamp DESC LIMIT 1")
    Optional<ActivityLog> findTopByUserAndActionTypeAndSuccessfulOrderByTimestampDesc(
            @Param("user") User user,
            @Param("actionType") ActionLogType actionType,
            @Param("successful") boolean successful);

    /**
     * Count by ActionLogType and success status (type-safe)
     */
    @Query("SELECT COUNT(al) FROM ActivityLog al WHERE " +
            "al.actionType = :#{#actionType.name()} AND " +
            "al.actionCategory = :#{#actionType.category} AND " +
            "al.successful = :successful AND " +
            "al.timestamp BETWEEN :startDate AND :endDate")
    long countByActionTypeAndSuccessfulAndTimestampBetween(
            @Param("actionType") ActionLogType actionType,
            @Param("successful") boolean successful,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find logs by ActionLogType and time range (type-safe)
     */
    @Query("SELECT al FROM ActivityLog al WHERE " +
            "al.actionType = :#{#actionType.name()} AND " +
            "al.actionCategory = :#{#actionType.category} AND " +
            "al.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY al.timestamp DESC")
    List<ActivityLog> findByActionTypeAndTimestampBetweenOrderByTimestampDesc(
            @Param("actionType") ActionLogType actionType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    // ================== CATEGORY-BASED METHODS ==================

    /**
     * Find logs by action category (AUTH, ADMIN, SECURITY, etc.)
     */
    List<ActivityLog> findByActionCategoryOrderByTimestampDesc(String actionCategory);

    /**
     * Find logs by user and category
     */
    List<ActivityLog> findByUserAndActionCategoryOrderByTimestampDesc(User user, String actionCategory);

    /**
     * Find logs by multiple categories
     */
    @Query("SELECT al FROM ActivityLog al WHERE " +
            "al.actionCategory IN :categories " +
            "ORDER BY al.timestamp DESC")
    List<ActivityLog> findByActionCategoryInOrderByTimestampDesc(@Param("categories") List<String> categories);

    /**
     * Count by category and time range
     */
    @Query("SELECT COUNT(al) FROM ActivityLog al WHERE " +
            "al.actionCategory = :category AND " +
            "al.timestamp BETWEEN :startDate AND :endDate")
    long countByActionCategoryAndTimestampBetween(
            @Param("category") String category,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    // ================== USER-BASED METHODS ==================

    /**
     * Find user's all logs
     */
    List<ActivityLog> findByUserOrderByTimestampDesc(User user);

    /**
     * Find user's logs by time range
     */
    List<ActivityLog> findByUserAndTimestampBetweenOrderByTimestampDesc(
            User user, Instant startDate, Instant endDate);

    /**
     * Find user's logs by success status
     */
    List<ActivityLog> findByUserAndSuccessfulOrderByTimestampDesc(User user, boolean successful);

    /**
     * Count user's actions in time range
     */
    long countByUserAndTimestampBetween(User user, Instant startDate, Instant endDate);

    // ================== TIME-BASED METHODS ==================

    /**
     * Find logs since timestamp
     */
    List<ActivityLog> findByTimestampAfterOrderByTimestampDesc(Instant timestamp);

    /**
     * Find logs in time range
     */
    List<ActivityLog> findByTimestampBetweenOrderByTimestampDesc(Instant startDate, Instant endDate);

    /**
     * Find logs before timestamp
     */
    List<ActivityLog> findByTimestampBeforeOrderByTimestampDesc(Instant timestamp);

    // ================== STATUS-BASED METHODS ==================

    /**
     * Find failed actions
     */
    List<ActivityLog> findBySuccessfulOrderByTimestampDesc(boolean successful);

    /**
     * Find failed actions since timestamp
     */
    List<ActivityLog> findBySuccessfulAndTimestampAfterOrderByTimestampDesc(boolean successful, Instant timestamp);

    /**
     * Find high-risk actions
     */
    @Query("SELECT al FROM ActivityLog al WHERE " +
            "al.riskLevel >= :minRiskLevel " +
            "ORDER BY al.riskLevel DESC, al.timestamp DESC")
    List<ActivityLog> findByRiskLevelGreaterThanEqualOrderByRiskLevelDescTimestampDesc(@Param("minRiskLevel") int minRiskLevel);

    /**
     * Find high-risk actions in time range
     */
    @Query("SELECT al FROM ActivityLog al WHERE " +
            "al.riskLevel >= :minRiskLevel AND " +
            "al.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY al.riskLevel DESC, al.timestamp DESC")
    List<ActivityLog> findHighRiskActionsByTimestamp(
            @Param("minRiskLevel") int minRiskLevel,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    // ================== IP/LOCATION-BASED METHODS ==================

    /**
     * Find logs by IP address
     */
    List<ActivityLog> findByIpAddressOrderByTimestampDesc(String ipAddress);

    /**
     * Find user's logs by IP address
     */
    List<ActivityLog> findByUserAndIpAddressOrderByTimestampDesc(User user, String ipAddress);

    /**
     * Find logs by location
     */
    List<ActivityLog> findByLocationOrderByTimestampDesc(String location);

    // ================== ADVANCED SEARCH ==================

    /**
     * Advanced search with multiple optional filters
     */
    @Query("SELECT al FROM ActivityLog al WHERE " +
            "(:userId IS NULL OR al.user.id = :userId) AND " +
            "(:ipAddress IS NULL OR al.ipAddress = :ipAddress) AND " +
            "(:category IS NULL OR al.actionCategory = :category) AND " +
            "(:successful IS NULL OR al.successful = :successful) AND " +
            "(:riskLevel IS NULL OR al.riskLevel >= :riskLevel) AND " +
            "al.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY al.timestamp DESC")
    List<ActivityLog> searchLogs(
            @Param("userId") Long userId,
            @Param("ipAddress") String ipAddress,
            @Param("category") String category,
            @Param("successful") Boolean successful,
            @Param("riskLevel") Integer riskLevel,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    // ================== STATISTICS ==================

    /**
     * Get action statistics by category
     */
    @Query("SELECT al.actionCategory, COUNT(al) FROM ActivityLog al WHERE " +
            "al.timestamp BETWEEN :startDate AND :endDate " +
            "GROUP BY al.actionCategory " +
            "ORDER BY COUNT(al) DESC")
    List<Object[]> getCategoryStatistics(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Get action statistics by type
     */
    @Query("SELECT al.actionType, al.actionCategory, COUNT(al) FROM ActivityLog al WHERE " +
            "al.timestamp BETWEEN :startDate AND :endDate " +
            "GROUP BY al.actionType, al.actionCategory " +
            "ORDER BY COUNT(al) DESC")
    List<Object[]> getActionTypeStatistics(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Count total logs in time range
     */
    long countByTimestampBetween(Instant startDate, Instant endDate);

    /**
     * Count active users in time range
     */
    @Query("SELECT COUNT(DISTINCT al.user.id) FROM ActivityLog al WHERE " +
            "al.timestamp BETWEEN :startDate AND :endDate")
    long countActiveUsers(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Count by success status in time range
     */
    long countBySuccessfulAndTimestampBetween(boolean successful, Instant startDate, Instant endDate);

    // ================== MAINTENANCE ==================

    /**
     * Delete old logs
     */
    @Modifying
    @Query("DELETE FROM ActivityLog al WHERE al.timestamp < :cutoffDate")
    long deleteByTimestampBefore(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Count logs before date (for maintenance info)
     */
    long countByTimestampBefore(Instant cutoffDate);

    /**
     * Find oldest logs (for archiving)
     */
    @Query("SELECT al FROM ActivityLog al ORDER BY al.timestamp ASC")
    List<ActivityLog> findOldestLogs();

    // ================== DEPRECATED METHODS (keep for migration) ==================

    /**
     * @deprecated Use findByActionTypeOrderByTimestampDesc(ActionLogType) instead
     */
    @Deprecated
    List<ActivityLog> findByActionTypeOrderByTimestampDesc(String actionType);

    /**
     * @deprecated Use findByUserAndActionTypeOrderByTimestampDesc(User, ActionLogType) instead
     */
    @Deprecated
    List<ActivityLog> findByUserAndActionTypeOrderByTimestampDesc(User user, String actionType);

    /**
     * @deprecated Use findTopByUserAndActionTypeOrderByTimestampDesc(User, ActionLogType) instead
     */
    @Deprecated
    Optional<ActivityLog> findTopByUserAndActionTypeOrderByTimestampDesc(User user, String actionType);

    /**
     * @deprecated Use countByActionTypeAndSuccessfulAndTimestampBetween(ActionLogType, boolean, Instant, Instant) instead
     */
    @Deprecated
    long countByUserAndActionTypeAndSuccessfulAndTimestampAfter(
            User user, String actionType, boolean successful, Instant timestamp);
}