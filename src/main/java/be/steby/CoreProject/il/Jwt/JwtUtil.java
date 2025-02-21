package be.steby.CoreProject.il.Jwt;

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
    private final String accessTokenCookieName; // renommé de cookieName
    private final String refreshTokenCookieName;

    public JwtUtil(
            @Value("${security.jwt.secret}") String secretKey,
            @Value("${security.jwt.access-token.expiration}") long accessTokenExpiration,
            @Value("${security.jwt.access-token.name}") String accessTokenCookieName,
            @Value("${security.jwt.refresh-token.name}") String refreshTokenCookieName)
    {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.accessTokenCookieName = accessTokenCookieName;
        this.refreshTokenCookieName = refreshTokenCookieName;
    }

    public String generateAccessToken(User user) {
        return generateToken(user, accessTokenExpiration);
    }

    private String generateToken(User user, long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername());
        claims.put("roles", user.getAuthorities());

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