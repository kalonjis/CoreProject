package be.steby.CoreProject.dal.repositories.tokens;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.BaseToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface BaseTokenRepository<T extends BaseToken> extends JpaRepository<T, Long> {
    Optional<T> findByIdAndTokenAndRevokedFalse(Long id, String token);

    @Modifying
    @Query("UPDATE #{#entityName} t SET t.revoked = true WHERE t.user = ?1")
    void revokeAllUserTokens(User user);

    @Modifying
    @Query("DELETE FROM #{#entityName} t WHERE t.expiryDate < ?1 OR t.revoked = true")
    void deleteExpiredTokens(Instant now);
}