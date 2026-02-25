package be.steby.CoreProject.dal.repositories.tokens;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.RefreshToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

/**
 * Repository for {@link RefreshToken} persistence operations.
 *
 * <p>Extends {@link BaseTokenRepository} which already provides:
 * {@code findByTokenIgnoreCase}, {@code findByUserAndRevokedFalse},
 * {@code findByIdAndRevokedFalse}, {@code findByIdAndTokenAndRevokedFalse},
 * {@code revokeAllUserTokens}, {@code deleteExpiredTokens} — via {@code #{#entityName}} JPQL.
 *
 * <p>Only device-aware and refresh-specific queries are declared here.
 */
public interface RefreshTokenRepository extends BaseTokenRepository<RefreshToken> {

    // =========================================================================
    // DEVICE-AWARE LOOKUP
    // =========================================================================

    /**
     * Returns all refresh tokens associated with a given user and device.
     *
     * @param user   the token owner
     * @param device the device to filter on
     * @return list of matching tokens, possibly empty
     */
    List<RefreshToken> findAllByUserAndDevice(User user, Device device);

    // =========================================================================
    // DEVICE-AWARE REVOCATION
    // =========================================================================

    /**
     * Marks all refresh tokens for a given user and device as revoked.
     * Returns the count of revoked tokens.
     *
     * @param user   the token owner
     * @param device the device whose tokens are revoked
     * @return number of tokens revoked
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = ?1 AND rt.device = ?2")
    int revokeAllByUserAndDevice(User user, Device device);

    /**
     * Marks all refresh tokens for a given user as revoked, except those
     * belonging to the specified device.
     *
     * @param user     the token owner
     * @param deviceId the ID of the device to exclude from revocation
     * @return number of tokens revoked
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken rt SET rt.revoked = true " +
            "WHERE rt.user = ?1 AND rt.device.id != ?2 AND rt.revoked = false")
    int revokeAllTokensExceptDevice(User user, Long deviceId);

    // =========================================================================
    // ANALYTICS
    // =========================================================================

    /**
     * Counts distinct users with at least one active (non-revoked, non-expired)
     * refresh token.
     *
     * @param now current timestamp used for expiry check
     * @return number of currently active users
     */
    @Query("SELECT COUNT(DISTINCT rt.user.id) FROM RefreshToken rt WHERE rt.revoked = false AND rt.expiryDate > ?1")
    long countActiveUsers(Instant now);
}