package be.steby.CoreProject.dal.repositories.tokens;


import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.tokens.RefreshToken;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends BaseTokenRepository<RefreshToken> {

    Optional<RefreshToken> findByIdAndTokenAndRevokedFalse(Long id, String token);

    Optional<RefreshToken> findByIdAndRevokedFalse(Long id);

    Optional<RefreshToken> findByToken(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < ?1 OR rt.revoked = true")
    void deleteExpiredTokens(Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = ?1")
    void revokeAllUserTokens(User user);


    List<RefreshToken> findAllByUserAndDevice(User user, Device device);

    Optional<RefreshToken> findByUserAndRevokedFalse(User user);


    @Query("SELECT COUNT(DISTINCT rt.user.id) FROM RefreshToken rt WHERE rt.revoked = false AND rt.expiryDate > ?1")
    long countActiveUsers(Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = ?1 AND rt.device = ?2")
    int revokeAllByUserAndDevice(User user, Device device);


}
