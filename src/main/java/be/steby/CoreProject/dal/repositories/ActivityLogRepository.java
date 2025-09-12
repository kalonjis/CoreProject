package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    /**
     * Find recent activity logs for a user ordered by timestamp
     */
    List<ActivityLog> findByUserOrderByTimestampDesc(User user);

    /**
     * Find activity logs by user and time range
     */
    @Query("SELECT al FROM ActivityLog al WHERE al.user = :user AND al.timestamp BETWEEN :startDate AND :endDate ORDER BY al.timestamp DESC")
    List<ActivityLog> findByUserAndTimestampBetween(
            @Param("user") User user,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Find activity logs by action type and category
     */
    List<ActivityLog> findByActionTypeAndActionCategoryOrderByTimestampDesc(String actionType, String actionCategory);
}