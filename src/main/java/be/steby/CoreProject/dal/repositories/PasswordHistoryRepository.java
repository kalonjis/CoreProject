package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.PasswordHistory;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    /**
     * Returns the N most recent password hashes for a user, ordered newest first.
     * Used to check if a new password has already been used.
     */
    @Query("""
        SELECT ph FROM PasswordHistory ph
        WHERE ph.user = :user
        ORDER BY ph.createdAt DESC
        LIMIT :limit
    """)
    List<PasswordHistory> findRecentByUser(@Param("user") User user, @Param("limit") int limit);

    /**
     * Deletes the oldest entries beyond the retention limit for a user.
     * Called after saving a new entry to keep the table lean.
     */
    @Modifying
    @Query("""
        DELETE FROM PasswordHistory ph
        WHERE ph.user = :user
        AND ph.id NOT IN (
            SELECT ph2.id FROM PasswordHistory ph2
            WHERE ph2.user = :user
            ORDER BY ph2.createdAt DESC
            LIMIT :keep
        )
    """)
    void deleteOldEntriesBeyondLimit(@Param("user") User user, @Param("keep") int keep);
}