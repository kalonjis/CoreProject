package be.steby.CoreProject.bll.services.security;

import be.steby.CoreProject.dl.entities.RefreshToken;
import be.steby.CoreProject.dl.entities.User;

import java.util.Optional;

public interface RefreshTokenService {
    int getRefreshTokenDurationInSeconds();
    RefreshToken createRefreshToken(User user);
    RefreshToken rotateRefreshToken(RefreshToken oldToken);
    Optional<RefreshToken> verifyToken(Long id, String token);
    RefreshToken verifyExpiration(RefreshToken token);
    void saveToken(RefreshToken token);
}
