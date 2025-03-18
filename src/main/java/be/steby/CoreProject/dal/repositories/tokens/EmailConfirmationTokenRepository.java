package be.steby.CoreProject.dal.repositories.tokens;


import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.EmailConfirmationToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface EmailConfirmationTokenRepository extends BaseTokenRepository<EmailConfirmationToken> {

    Optional<EmailConfirmationToken> findByIdAndTokenAndRevokedFalse(Long id, String token);

    Optional<EmailConfirmationToken> findByIdAndRevokedFalse(Long id);

    Optional<EmailConfirmationToken> findByToken(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM EmailConfirmationToken ect WHERE ect.expiryDate < ?1 OR ect.revoked = true")
    void deleteExpiredTokens(Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE EmailConfirmationToken ect SET ect.revoked = true WHERE ect.user = ?1")
    void revokeAllUserTokens(User user);

    Optional<EmailConfirmationToken> findByUserAndRevokedFalse(User user);
}
