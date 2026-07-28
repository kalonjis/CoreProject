package be.steby.CoreProject.integration.password;

import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dal.repositories.tokens.PasswordResetTokenRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import be.steby.CoreProject.utils.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the password reset flow (EMAIL_LINK):
 * forgot → token → reset → login.
 *
 * <p>Runs with the full Spring context (H2, real filters). Mail sending
 * ({@code PasswordNotificationListener}) is {@code @Async} and never awaited —
 * the reset token is created synchronously inside
 * {@code EmailLinkPasswordResetService#requestReset}, before the event is
 * published, so it is already in the database by the time the HTTP response
 * comes back. Only the EMAIL_LINK flow is covered here — EMAIL_CODE and
 * SMS_CODE add a JWT verification-cookie layer and are left for a dedicated
 * pass if that flow regresses.
 *
 * <p>Covers:
 * <ol>
 *   <li>Forgot-password guards — unknown email (anti-enumeration), already authenticated</li>
 *   <li>Reset guards — malformed token, reused token after a successful reset</li>
 *   <li>Nominal flow — forgot creates a token, reset changes the password and
 *       revokes both the token and every refresh token, and login works with
 *       the new password only</li>
 * </ol>
 */
class PasswordResetIntegrationTest extends IntegrationTestBase {

    private static final String FORGOT_URL  = "/api/password/forgot/email-link";
    private static final String RESET_URL   = "/api/password/reset";
    private static final String LOGIN_URL   = "/api/auth/login";
    private static final String REFRESH_URL = "/api/auth/refresh-token";
    private static final String OLD_PASSWORD = "TestPass123!";
    private static final String NEW_PASSWORD = "NouveauPass456!";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Persists a fully-activated user that can log in without 2FA. */
    private User createActiveUser(String username) {
        User user = new User(username, username + "@integration-test.com",
                passwordEncoder.encode(OLD_PASSWORD));
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setEverActivated(true);
        user.setTwoFactorEnabled(false);
        user.setMustChangePassword(false);
        user.setActivatedAt(Instant.now());
        return userRepository.save(user);
    }

    /** Extracts a named cookie, falling back to Set-Cookie header parsing. */
    private Cookie extractCookie(MvcResult result, String name) {
        Cookie cookie = result.getResponse().getCookie(name);
        if (cookie != null) return cookie;

        for (String header : result.getResponse().getHeaders("Set-Cookie")) {
            String prefix = name + "=";
            if (header.startsWith(prefix)) {
                return new Cookie(name, header.split(";")[0].substring(prefix.length()));
            }
        }
        return null;
    }

    private MvcResult performLogin(String username, String password) throws Exception {
        return mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")
                        .header("User-Agent", USER_AGENT))
                .andReturn();
    }

    /** POSTs the forgot-password endpoint and returns the raw MvcResult (no status assertion). */
    private MvcResult performForgot(String email, Cookie... cookies) throws Exception {
        MockHttpServletRequestBuilder request = post(FORGOT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}");
        if (cookies.length > 0) {
            request.cookie(cookies);
        }
        return mockMvc.perform(request).andReturn();
    }

    /** PUTs the reset endpoint with a new password and returns the raw MvcResult (no status assertion). */
    private MvcResult performReset(String token, String password, String confirmPassword) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.put(RESET_URL)
                        .param("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + password + "\",\"confirmPassword\":\"" + confirmPassword + "\"}"))
                .andReturn();
    }

    /** Triggers forgot-password for an existing user and returns the token's encrypted publicId (the value carried by the email link). */
    private String requestResetToken(User user) throws Exception {
        assertEquals(200, performForgot(user.getEmail()).getResponse().getStatus());
        return passwordResetTokenRepository
                .findByUserAndTokenTypeAndRevokedFalse(user, TokenType.PASSWORD_RESET)
                .map(PasswordResetToken::getPublicId)
                .orElseThrow();
    }

    // =========================================================================
    // Forgot password — guards métier
    // =========================================================================

    @Nested
    class ForgotPasswordGuards {

        @Test
        void emailInconnu_retourne200SansCreerDeToken() throws Exception {
            long tokensBefore = passwordResetTokenRepository.count();

            MvcResult result = performForgot("inconnu@integration-test.com");

            // Anti-enumeration : ne jamais révéler si l'email existe ou non.
            assertEquals(200, result.getResponse().getStatus());
            assertEquals(tokensBefore, passwordResetTokenRepository.count(),
                    "Aucun token ne doit être créé pour un email inconnu");
        }

        @Test
        void utilisateurAuthentifie_retourne403() throws Exception {
            User user = createActiveUser("pwdreset_forgot_auth");
            MvcResult login = performLogin("pwdreset_forgot_auth", OLD_PASSWORD);
            Cookie accessToken = extractCookie(login, "access_token");
            assertNotNull(accessToken);

            MvcResult result = performForgot(user.getEmail(), accessToken);

            assertEquals(403, result.getResponse().getStatus());
        }
    }

    // =========================================================================
    // Reset — guards métier
    // =========================================================================

    @Nested
    class ResetGuards {

        @Test
        void tokenMalFormé_retourne400() throws Exception {
            MvcResult result = performReset("ceci-n-est-pas-un-token-chiffre", NEW_PASSWORD, NEW_PASSWORD);

            assertEquals(400, result.getResponse().getStatus());
        }

        @Test
        void motsDePasseDifferents_retourne400() throws Exception {
            // La validation du body (@PasswordMatch) échoue avant même la résolution du token.
            MvcResult result = performReset("peu-importe", NEW_PASSWORD, "AutreMotDePasse789!");

            assertEquals(400, result.getResponse().getStatus());
        }

        @Test
        void tokenReutiliseApresReset_retourne404() throws Exception {
            User user = createActiveUser("pwdreset_reuse");
            String token = requestResetToken(user);

            assertEquals(200, performReset(token, NEW_PASSWORD, NEW_PASSWORD).getResponse().getStatus());

            MvcResult second = performReset(token, "EncoreUnAutre123!", "EncoreUnAutre123!");

            assertEquals(404, second.getResponse().getStatus(),
                    "Le token doit être révoqué après usage — un rejeu ne doit pas retrouver de token valide");
        }
    }

    // =========================================================================
    // Cycle nominal — forgot → reset → login
    // =========================================================================

    @Nested
    class CycleNominal {

        @Test
        void forgotPassword_creeUnTokenPourUtilisateurExistant() throws Exception {
            User user = createActiveUser("pwdreset_nominal_forgot");

            String token = requestResetToken(user);

            assertNotNull(token);
            assertFalse(passwordResetTokenRepository
                    .findByUserAndTokenTypeAndRevokedFalse(user, TokenType.PASSWORD_RESET)
                    .orElseThrow()
                    .isRevoked());
        }

        @Test
        void reset_changeLeMotDePasseRevoqueLeTokenEtDeconnecteLesAutresAppareils() throws Exception {
            User user = createActiveUser("pwdreset_nominal_reset");
            MvcResult login = performLogin("pwdreset_nominal_reset", OLD_PASSWORD);
            Cookie refreshToken = extractCookie(login, "refresh_token");
            assertNotNull(refreshToken);

            String token = requestResetToken(user);

            assertEquals(200, performReset(token, NEW_PASSWORD, NEW_PASSWORD).getResponse().getStatus());

            User updated = userRepository.findByUsernameIgnoreCase("pwdreset_nominal_reset").orElseThrow();
            assertTrue(passwordEncoder.matches(NEW_PASSWORD, updated.getPassword()));
            assertNotNull(updated.getPasswordChangedAt());

            assertTrue(passwordResetTokenRepository
                            .findByUserAndTokenTypeAndRevokedFalse(updated, TokenType.PASSWORD_RESET)
                            .isEmpty(),
                    "Le token doit être révoqué après reset");

            // Tous les appareils doivent être déconnectés : l'ancien refresh_token ne doit plus fonctionner.
            // 404 (pas 401) : un refresh_token révoqué est traité comme introuvable par
            // getInternalValidToken — le 401 InvalidRefreshTokenException ne concerne que
            // le cookie absent (cf. AuthFlowIntegrationTest.refreshAbsent_retourne401).
            mockMvc.perform(post(REFRESH_URL).cookie(refreshToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        void flowComplet_forgotResetLogin() throws Exception {
            String username = "pwdreset_full_flow";
            User user = createActiveUser(username);

            String token = requestResetToken(user);
            assertEquals(200, performReset(token, NEW_PASSWORD, NEW_PASSWORD).getResponse().getStatus());

            assertEquals(200, performLogin(username, NEW_PASSWORD).getResponse().getStatus(),
                    "Le login doit réussir avec le nouveau mot de passe");
            assertEquals(401, performLogin(username, OLD_PASSWORD).getResponse().getStatus(),
                    "L'ancien mot de passe ne doit plus fonctionner");
        }
    }
}
