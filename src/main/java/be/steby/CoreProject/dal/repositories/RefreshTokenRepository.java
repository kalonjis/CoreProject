package be.steby.CoreProject.dal.repositories;


import be.steby.CoreProject.dl.entities.RefreshToken;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByIdAndTokenAndRevokedFalse(Long id, String token);

    Optional<RefreshToken> findByIdAndRevokedFalse(Long id);

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < ?1 OR rt.revoked = true")
    void deleteExpiredTokens(Instant now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = ?1")
    void revokeAllUserTokens(User user);

    Optional<RefreshToken> findByUserAndRevokedFalse(User user);
}
