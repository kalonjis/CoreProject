// src/test/java/be/steby/CoreProject/il/Jwt/JwtUtilTest.java
package be.steby.CoreProject.il.Jwt;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires pour JwtUtil.
 * On teste la génération et validation des JWT tokens.
 */
@DisplayName("JwtUtil - Tests de génération et validation JWT")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // On crée JwtUtil avec des valeurs de test
        jwtUtil = new JwtUtil(
                "test-secret-key-for-testing-purposes-only-minimum-256-bits-long",
                3600000L,  // 1 heure
                "access_token",
                "refresh_token"
        );
    }

    @Test
    @DisplayName("Génération de token avec utilisateur et device valides")
    void generateToken_withValidUserAndDevice_shouldReturnValidToken() {
        // ARRANGE
        User user = createTestUser();
        Device device = createTestDevice();
        // On simule un ID pour le device avec la réflexion
        setDeviceId(device, 456L);

        // ACT
        String token = jwtUtil.generateAccessToken(user, device);

        // ASSERT
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        // Un JWT a 3 parties séparées par des points
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Validation d'un token valide doit retourner les claims")
    void validateToken_withValidToken_shouldReturnClaims() {
        // ARRANGE
        User user = createTestUser();
        Device device = createTestDevice();
        // On simule un ID pour le device avec la réflexion
        setDeviceId(device, 123L);

        String token = jwtUtil.generateAccessToken(user, device);

        // ACT
        Claims claims = jwtUtil.validateToken(token);

        // ASSERT
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(user.getUsername());
        assertThat(claims.get("username", String.class)).isEqualTo("testuser");
        assertThat(claims.get("deviceId", Long.class)).isEqualTo(123L);
        assertThat(claims.get("deviceFingerprint", String.class)).isEqualTo("test-fingerprint");
        assertThat(claims.get("mustChangePassword", Boolean.class)).isFalse();
    }

    @Test
    @DisplayName("Validation d'un token invalide doit lever une exception")
    void validateToken_withInvalidToken_shouldThrowException() {
        // ARRANGE
        String invalidToken = "invalid.token.here";

        // ACT & ASSERT
        assertThatThrownBy(() -> jwtUtil.validateToken(invalidToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Token généré doit avoir une date d'expiration future")
    void generatedToken_shouldHaveFutureExpirationDate() {
        // ARRANGE
        User user = createTestUser();
        Device device = createTestDevice();
        setDeviceId(device, 789L); // Simuler un ID avec la réflexion
        long beforeGeneration = System.currentTimeMillis();

        // ACT
        String token = jwtUtil.generateAccessToken(user, device);
        Claims claims = jwtUtil.validateToken(token);

        // ASSERT
        assertThat(claims.getExpiration().getTime())
                .isGreaterThan(beforeGeneration + 3500000); // Au moins 58 minutes dans le futur
    }

    // ========================================
    // Méthodes utilitaires pour créer des objets de test
    // ========================================

    private User createTestUser() {
        // Utilise le constructeur existant de User
        User user = new User("testuser", "test@example.com", "password");
        user.setMustChangePassword(false);
        return user;
    }

    private Device createTestDevice() {
        // Utilise le builder pattern de Device
        return Device.builder()
                .fingerprint("test-fingerprint")
                .confirmed(true)
                .deviceType("Desktop")
                .browser("Chrome")
                .operatingSystem("Windows")
                .build();
    }

    /**
     * Méthode utilitaire SÉCURISÉE pour définir l'ID d'un Device.
     * Utilise les utilitaires Spring Test au lieu de la réflexion pure.
     */
    private void setDeviceId(Device device, Long id) {
        ReflectionTestUtils.setField(device, "id", id);
    }
}