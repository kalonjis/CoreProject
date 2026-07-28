package be.steby.CoreProject.integration.auth;

import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.utils.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the core authentication flow:
 * Login → Access protected route → Refresh token → Logout.
 *
 * <p>Uses the full Spring context with H2, real JWT filters, and real CSRF.
 * Covers the flows most likely to regress on security-related refactors.
 */
class AuthFlowIntegrationTest extends IntegrationTestBase {

    private static final String TEST_PASSWORD = "TestPass123!";
    private static final String LOGIN_URL    = "/api/auth/login";
    private static final String REFRESH_URL  = "/api/auth/refresh-token";
    private static final String LOGOUT_URL   = "/api/auth/logout";
    private static final String ME_URL       = "/api/auth/me";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Persists a fully-activated user that can log in without 2FA.
     * Username must be unique per test (no rollback between methods).
     */
    private User createActiveUser(String username) {
        User user = new User(username, username + "@integration-test.com",
                passwordEncoder.encode(TEST_PASSWORD));
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setEverActivated(true);
        user.setTwoFactorEnabled(false);
        user.setMustChangePassword(false);
        user.setActivatedAt(Instant.now());
        return userRepository.save(user);
    }

    /** Performs a standard login request and expects HTTP 200. */
    private MvcResult performLogin(String username) throws Exception {
        return mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + TEST_PASSWORD + "\"}")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0"))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * Extracts a named cookie from a MockMvc response.
     *
     * <p>BaseCookieService writes cookies via {@code response.addHeader("Set-Cookie", ...)}
     * rather than {@code addCookie()}. Spring's MockHttpServletResponse parses Set-Cookie
     * headers and populates the cookie map, so {@code getCookie()} normally works.
     * This helper falls back to manual header parsing if the cookie map is empty.
     */
    private Cookie extractCookie(MvcResult result, String name) {
        Cookie cookie = result.getResponse().getCookie(name);
        if (cookie != null) return cookie;

        for (String header : result.getResponse().getHeaders("Set-Cookie")) {
            String prefix = name + "=";
            if (header.startsWith(prefix)) {
                String value = header.split(";")[0].substring(prefix.length());
                Cookie parsed = new Cookie(name, value);
                for (String part : header.split(";")) {
                    String trimmed = part.trim();
                    if (trimmed.toLowerCase().startsWith("max-age=")) {
                        try {
                            parsed.setMaxAge(Integer.parseInt(trimmed.substring("max-age=".length())));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                return parsed;
            }
        }
        return null;
    }

    /** Checks whether a Set-Cookie header clears the given cookie (Max-Age=0). */
    private boolean cookieIsCleared(List<String> setCookieHeaders, String cookieName) {
        return setCookieHeaders.stream()
                .anyMatch(h -> h.startsWith(cookieName + "=") && h.contains("Max-Age=0"));
    }

    // =========================================================================
    // Route protégée — sans token
    // =========================================================================

    @Nested
    class RouteProtégée {

        @Test
        void sansToken_retourne401() throws Exception {
            mockMvc.perform(get(ME_URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void avecTokenJwtInvalide_retourne401() throws Exception {
            mockMvc.perform(get(ME_URL)
                            .cookie(new Cookie("access_token", "en-tete.payload.signature-invalide")))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // Login
    // =========================================================================

    @Nested
    class Login {

        @Test
        void identifiantsValides_retourne200AvecLesTroisCookies() throws Exception {
            createActiveUser("login_ok");

            MvcResult result = performLogin("login_ok");

            assertNotNull(extractCookie(result, "access_token"),  "access_token manquant");
            assertNotNull(extractCookie(result, "refresh_token"), "refresh_token manquant");
            assertNotNull(extractCookie(result, "XSRF-TOKEN"),    "XSRF-TOKEN manquant");
        }

        @Test
        void motDePasseErroné_retourne401() throws Exception {
            createActiveUser("login_bad_pwd");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"login_bad_pwd\",\"password\":\"MauvaisMotDePasse!\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void utilisateurInexistant_retourne401() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"utilisateur_inexistant\",\"password\":\"TestPass123!\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void compteNonActivé_retourne403() throws Exception {
            // Utilisateur créé via le constructeur standard : enabled=false, everActivated=false
            User inactive = new User("login_not_activated",
                    "not_activated@integration-test.com",
                    passwordEncoder.encode(TEST_PASSWORD));
            userRepository.save(inactive);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"login_not_activated\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // Accès route protégée avec access token
    // =========================================================================

    @Nested
    class AccèsAvecToken {

        @Test
        void accessTokenValide_retourne200AvecUsername() throws Exception {
            createActiveUser("me_flow");

            MvcResult loginResult = performLogin("me_flow");
            Cookie accessToken = extractCookie(loginResult, "access_token");
            assertNotNull(accessToken);

            mockMvc.perform(get(ME_URL).cookie(accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("me_flow"));
        }
    }

    // =========================================================================
    // Refresh token
    // =========================================================================

    @Nested
    class RefreshToken {

        @Test
        void refreshValide_retourneNouveauAccessToken() throws Exception {
            createActiveUser("refresh_flow");

            MvcResult loginResult = performLogin("refresh_flow");
            Cookie refreshToken = extractCookie(loginResult, "refresh_token");
            assertNotNull(refreshToken);

            // refresh-token est dans CSRF_IGNORE → pas de XSRF-TOKEN requis
            MvcResult refreshResult = mockMvc.perform(post(REFRESH_URL)
                            .cookie(refreshToken))
                    .andExpect(status().isOk())
                    .andReturn();

            assertNotNull(extractCookie(refreshResult, "access_token"),
                    "Un nouveau access_token doit être émis après refresh");
        }

        @Test
        void refreshAbsent_retourne401() throws Exception {
            mockMvc.perform(post(REFRESH_URL))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // Logout
    // =========================================================================

    @Nested
    class Logout {

        @Test
        void avecTokensValides_retourne200EtSupprimeLesTokens() throws Exception {
            createActiveUser("logout_flow");

            MvcResult loginResult = performLogin("logout_flow");
            Cookie accessToken  = extractCookie(loginResult, "access_token");
            Cookie refreshToken = extractCookie(loginResult, "refresh_token");
            Cookie xsrfToken    = extractCookie(loginResult, "XSRF-TOKEN");
            assertNotNull(accessToken);
            assertNotNull(refreshToken);
            assertNotNull(xsrfToken);

            // logout nécessite le XSRF-TOKEN (pas dans CSRF_IGNORE)
            MvcResult logoutResult = mockMvc.perform(post(LOGOUT_URL)
                            .cookie(accessToken, refreshToken, xsrfToken)
                            .header("X-XSRF-TOKEN", xsrfToken.getValue()))
                    .andExpect(status().isOk())
                    .andReturn();

            List<String> setCookieHeaders = logoutResult.getResponse().getHeaders("Set-Cookie");
            assertTrue(cookieIsCleared(setCookieHeaders, "access_token"),
                    "access_token doit être effacé (Max-Age=0) après logout");
            assertTrue(cookieIsCleared(setCookieHeaders, "refresh_token"),
                    "refresh_token doit être effacé (Max-Age=0) après logout");
        }
    }
}
