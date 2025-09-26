package be.steby.CoreProject.bll.common.services.tokens;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.BaseToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;

import java.util.Optional;

public interface BaseTokenService<T extends BaseToken> {

    // =========================================================================
    // SECURE METHODS - For URL/external access (encrypted tokens)
    // =========================================================================
    T getSecureToken(String tokenValue);
    T getSecureValidToken(String tokenValue, TokenType tokenType);
    T getSecureTokenByType(String tokenValue, TokenType tokenType);

    // =========================================================================
    // INTERNAL METHODS - For business logic (plain tokens)
    // =========================================================================
    T getInternalToken(String plainToken);
    T getInternalValidToken(String plainToken, TokenType tokenType);
    T getInternalTokenByType(String plainToken, TokenType tokenType);

    // =========================================================================
    // TOKEN MANAGEMENT METHODS
    // =========================================================================
    T createToken(User user, Long expirationInMillis, boolean revokeExisting);
    T createToken(User user, TokenType tokenType, Long expirationInMillis, boolean revokeExisting);
    T createToken(User user, Long expirationInMillis);
    Optional<T> verifyToken(Long id, String token);
    T verifyTokenValidity(T token);
    void revokeAllUserTokens(User user);
    void revokeToken(T token);
    void saveToken(T token);
}