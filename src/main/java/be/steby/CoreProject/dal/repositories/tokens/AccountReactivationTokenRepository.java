package be.steby.CoreProject.dal.repositories.tokens;


import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;
import be.steby.CoreProject.dl.entities.tokens.AccountReactivationToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface AccountReactivationTokenRepository extends BaseTokenRepository<AccountReactivationToken> {

    Optional<AccountReactivationToken> findByIdAndTokenAndRevokedFalse(Long id, String token);

    Optional<AccountReactivationToken> findByIdAndRevokedFalse(Long id);

    Optional<AccountReactivationToken> findByToken(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM AccountReactivationToken art WHERE art.expiryDate < ?1 OR art.revoked = true")
    void deleteExpiredTokens(Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AccountReactivationToken art SET art.revoked = true WHERE art.user = ?1")
    void revokeAllUserTokens(User user);

    Optional<AccountReactivationToken> findByUserAndRevokedFalse(User user);
}
