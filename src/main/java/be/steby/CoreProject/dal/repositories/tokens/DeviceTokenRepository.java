package be.steby.CoreProject.dal.repositories.tokens;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.DeviceConfirmationToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface DeviceTokenRepository extends BaseTokenRepository<DeviceConfirmationToken> {

    Optional<DeviceConfirmationToken> findByIdAndTokenAndRevokedFalse(Long id, String token);

    Optional<DeviceConfirmationToken> findByIdAndRevokedFalse(Long id);

    Optional<DeviceConfirmationToken> findByToken(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM DeviceConfirmationToken t WHERE t.expiryDate < ?1 OR t.revoked = true")
    void deleteExpiredTokens(Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DeviceConfirmationToken t SET t.revoked = true WHERE t.user = ?1")
    void revokeAllUserTokens(User user);

    Optional<DeviceConfirmationToken> findByUserAndRevokedFalse(User user);
}
