package be.steby.CoreProject.il.Jwt;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Getter
public class JwtUtil {

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final String accessTokenCookieName;
    private final String refreshTokenCookieName;

    public JwtUtil(JwtProperties jwtProperties) {
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
        this.accessTokenExpiration = jwtProperties.getAccessToken().getExpiration();
        this.accessTokenCookieName = jwtProperties.getAccessToken().getName();
        this.refreshTokenCookieName = jwtProperties.getRefreshToken().getName();
    }


    public String generateAccessToken(User user, Device device) {
        return generateToken(user, device, accessTokenExpiration);
    }

    private String generateToken(User user, Device device, long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername());
        claims.put("mustChangePassword", user.isMustChangePassword());
        claims.put("roles", user.getAuthorities());

        // Ajouter les informations du Device
        claims.put("deviceId", device.getId());
        claims.put("deviceFingerprint", device.getFingerprint());
        claims.put("deviceTrustLevel", device.getDeviceTrustLevel().name());
        claims.put("deviceConfirmed", device.isConfirmed());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}