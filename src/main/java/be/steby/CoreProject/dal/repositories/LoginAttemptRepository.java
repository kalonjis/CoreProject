package be.steby.CoreProject.dl.repositories;

import be.steby.CoreProject.dl.entities.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /**
     * Find attempt record by username and type
     */
    Optional<LoginAttempt> findByUsernameAndAttemptType(String username, String attemptType);

    /**
     * Find attempt record by IP address and type
     */
    Optional<LoginAttempt> findByIpAddressAndAttemptType(String ipAddress, String attemptType);

    /**
     * Find attempt record by username, IP address and type (for combined tracking)
     */
    Optional<LoginAttempt> findByUsernameAndIpAddressAndAttemptType(
            String username, String ipAddress, String attemptType);

    /**
     * Find all active blocks (blocked_until > now)
     */
    @Query("SELECT la FROM LoginAttempt la WHERE la.isBlocked = true AND la.blockedUntil > :now")
    List<LoginAttempt> findActiveBlocks(@Param("now") Instant now);

    /**
     * Clear expired blocks
     */
    @Modifying
    @Query("UPDATE LoginAttempt la SET la.isBlocked = false, la.blockedUntil = null " +
            "WHERE la.isBlocked = true AND la.blockedUntil <= :now")
    int clearExpiredBlocks(@Param("now") Instant now);

    /**
     * Delete old attempt records (cleanup)
     */
    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.lastAttemptTime < :cutoffTime AND la.isBlocked = false")
    int deleteOldAttempts(@Param("cutoffTime") Instant cutoffTime);

    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.blockedUntil < :expiredBefore")
    int deleteByBlockedUntilBefore(@Param("expiredBefore") Instant expiredBefore);
}