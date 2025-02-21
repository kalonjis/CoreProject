package be.steby.CoreProject.dal.repositories.tokens;


import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface AccountConfirmationTokenRepository extends BaseTokenRepository<AccountConfirmationToken> {

    Optional<AccountConfirmationToken> findByIdAndTokenAndRevokedFalse(Long id, String token);

    Optional<AccountConfirmationToken> findByIdAndRevokedFalse(Long id);

    Optional<AccountConfirmationToken> findByToken(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM AccountConfirmationToken act WHERE act.expiryDate < ?1 OR act.revoked = true")
    void deleteExpiredTokens(Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AccountConfirmationToken act SET act.revoked = true WHERE act.user = ?1")
    void revokeAllUserTokens(User user);

    Optional<AccountConfirmationToken> findByUserAndRevokedFalse(User user);
}
