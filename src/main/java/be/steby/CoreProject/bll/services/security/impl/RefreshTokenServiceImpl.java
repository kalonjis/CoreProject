package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.bll.exceptions.TokenRefreshExpiredException;
import be.steby.CoreProject.bll.exceptions.TokenRefreshRevokedException;
import be.steby.CoreProject.bll.services.security.RefreshTokenService;
import be.steby.CoreProject.dal.repositories.RefreshTokenRepository;
import be.steby.CoreProject.dl.entities.RefreshToken;
import be.steby.CoreProject.dl.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${security.jwt.refresh-token.expiration}")
    private Long refreshTokenDurationMs;


    @Override
    public int getRefreshTokenDurationInSeconds() {
        long durationInSeconds = refreshTokenDurationMs / 1000;
        if (durationInSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("Refresh token duration too large for cookie max age");
        }
        return (int) durationInSeconds;
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Révoquer tous les tokens existants de l'utilisateur
        refreshTokenRepository.revokeAllUserTokens(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(generateSecureToken());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
        // Révoquer l'ancien token
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        // Créer un nouveau token pour l'utilisateur
        return createRefreshToken(oldToken.getUser());
    }

    @Override
    public Optional<RefreshToken> verifyToken(Long id, String token) {
        return refreshTokenRepository.findByIdAndTokenAndRevokedFalse(id, token);
    }

    @Override
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked()) {
            throw new TokenRefreshRevokedException("Refresh token was revoked. Please sign in again");
        }

        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new TokenRefreshExpiredException("Refresh token was expired. Please sign in again");
        }

        return token;
    }


    @Override
    public void saveToken(RefreshToken token) {
        refreshTokenRepository.save(token);
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * *") // Toutes les heures
    public void cleanExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(Instant.now());
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}