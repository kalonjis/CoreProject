package be.steby.CoreProject.dal.repositories.tokens;


import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.AccountDeactivationToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface AccountDeactivationTokenRepository extends BaseTokenRepository<AccountDeactivationToken> {

    Optional<AccountDeactivationToken> findByIdAndTokenAndRevokedFalse(Long id, String token);

    Optional<AccountDeactivationToken> findByIdAndRevokedFalse(Long id);

    Optional<AccountDeactivationToken> findByToken(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM AccountDeactivationToken adt WHERE act.expiryDate < ?1 OR adt.revoked = true")
    void deleteExpiredTokens(Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AccountDeactivationToken adt SET adt.revoked = true WHERE adt.user = ?1")
    void revokeAllUserTokens(User user);

    Optional<AccountDeactivationToken> findByUserAndRevokedFalse(User user);
}
