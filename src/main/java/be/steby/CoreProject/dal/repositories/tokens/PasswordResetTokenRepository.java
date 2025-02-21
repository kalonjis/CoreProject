package be.steby.CoreProject.dal.repositories.tokens;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends BaseTokenRepository<PasswordResetToken> {

    Optional<PasswordResetToken> findByIdAndTokenAndRevokedFalse(Long id, String token);

    Optional<PasswordResetToken> findByIdAndRevokedFalse(Long id);

    Optional<PasswordResetToken> findByToken(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PasswordResetToken rt WHERE rt.expiryDate < ?1 OR rt.revoked = true")
    void deleteExpiredTokens(Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PasswordResetToken rt SET rt.revoked = true WHERE rt.user = ?1")
    void revokeAllUserTokens(User user);

    Optional<PasswordResetToken> findByUserAndRevokedFalse(User user);
}
