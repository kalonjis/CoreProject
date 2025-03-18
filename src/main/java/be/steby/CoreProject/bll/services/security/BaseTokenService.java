package be.steby.CoreProject.bll.services.security;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.BaseToken;

import java.util.Optional;

public interface BaseTokenService<T extends BaseToken> {
    T getToken(String token);
    T createToken(User user, Long expirationInMillis, boolean revokeExisting);
    T createToken(User user, Long expirationInMillis);
//    T rotateToken(T oldToken);
    Optional<T> verifyToken(Long id, String token);
    T verifyTokenValidity(T token);
    void revokeAllUserTokens(User user);
    void revokeToken(T token);
    void saveToken(T token);
}
