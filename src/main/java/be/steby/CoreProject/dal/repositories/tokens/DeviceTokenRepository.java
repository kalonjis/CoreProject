package be.steby.CoreProject.dal.repositories.tokens;

import be.steby.CoreProject.dl.entities.tokens.DeviceConfirmationToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for {@link DeviceConfirmationToken} persistence operations.
 *
 * <p>Extends {@link BaseTokenRepository} which already provides:
 * {@code findByTokenIgnoreCase}, {@code findByUserAndRevokedFalse},
 * {@code findByIdAndRevokedFalse}, {@code findByIdAndTokenAndRevokedFalse},
 * {@code revokeAllUserTokens}, {@code deleteExpiredTokens} — via {@code #{#entityName}} JPQL.
 *
 * <p>Only device-aware queries are declared here.
 */
public interface DeviceTokenRepository extends BaseTokenRepository<DeviceConfirmationToken> {

    // =========================================================================
    // DEVICE-AWARE LOOKUP
    // =========================================================================

    /**
     * Returns all confirmation tokens associated with a given user and device.
     *
     * @param userId   the user's database ID
     * @param deviceId the device's database ID
     * @return list of matching tokens, possibly empty
     */
    List<DeviceConfirmationToken> findAllByUserIdAndDeviceId(Long userId, Long deviceId);

    // =========================================================================
    // DEVICE-AWARE REVOCATION
    // =========================================================================

    /**
     * Marks all active (non-revoked) confirmation tokens for a given user
     * and device as revoked. Returns the count of revoked tokens.
     *
     * @param userId   the user's database ID
     * @param deviceId the device's database ID
     * @return number of tokens revoked
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DeviceConfirmationToken t SET t.revoked = true " +
            "WHERE t.user.id = :userId AND t.deviceId = :deviceId AND t.revoked = false")
    int revokeAllByUserAndDevice(@Param("userId") Long userId, @Param("deviceId") Long deviceId);
}