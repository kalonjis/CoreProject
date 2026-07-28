package be.steby.CoreProject.integration.account;

import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dal.repositories.tokens.AccountConfirmationTokenRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import be.steby.CoreProject.utils.IntegrationTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the account lifecycle flow:
 * Signup → activation → login.
 *
 * <p>Runs with the full Spring context (H2, real filters). Mail sending
 * ({@code AccountNotificationListener}) is {@code @Async} and never awaited —
 * the confirmation token is created synchronously inside
 * {@code AccountServiceImpl#signup}, before the event is published, so it is
 * already in the database by the time the HTTP response comes back.
 *
 * <p>Covers:
 * <ol>
 *   <li>Signup guards — password mismatch, username already taken</li>
 *   <li>Activation guards — malformed token, reuse after activation</li>
 *   <li>Nominal flow — signup creates a disabled user + confirmation token,
 *       activation enables it, and the newly-activated user can log in</li>
 * </ol>
 */
class SignupActivationIntegrationTest extends IntegrationTestBase {

    private static final String SIGNUP_URL   = "/api/account/signup";
    private static final String ACTIVATE_URL = "/api/account/activate";
    private static final String RESEND_URL   = "/api/account/resend-activation";
    private static final String LOGIN_URL    = "/api/auth/login";
    private static final String TEST_PASSWORD = "TestPass123!";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountConfirmationTokenRepository confirmationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // =========================================================================
    // Helpers
    // =========================================================================

    private String signupBody(String username, String password, String confirmPassword) {
        return "{\"username\":\"" + username + "\","
                + "\"email\":\"" + username + "@integration-test.com\","
                + "\"password\":\"" + password + "\","
                + "\"confirmPassword\":\"" + confirmPassword + "\"}";
    }

    /** POSTs a signup request and returns the raw MvcResult (no status assertion). */
    private MvcResult performSignup(String username) throws Exception {
        return mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(username, TEST_PASSWORD, TEST_PASSWORD))
                        .header("User-Agent", USER_AGENT))
                .andReturn();
    }

    /** Signs up a user and expects 201. Returns the persisted (disabled) entity. */
    private User signup(String username) throws Exception {
        MvcResult result = performSignup(username);
        assertEquals(201, result.getResponse().getStatus());
        return userRepository.findByUsernameIgnoreCase(username).orElseThrow();
    }

    /** Reads the active ACCOUNT_CONFIRMATION token's encrypted publicId for a user — the value the activation link carries. */
    private String activationTokenFor(User user) {
        return confirmationTokenRepository
                .findByUserAndTokenTypeAndRevokedFalse(user, TokenType.ACCOUNT_CONFIRMATION)
                .map(AccountConfirmationToken::getPublicId)
                .orElseThrow();
    }

    /** GETs the activation endpoint and returns the raw MvcResult (no status assertion). */
    private MvcResult performActivate(String token) throws Exception {
        return mockMvc.perform(get(ACTIVATE_URL).param("token", token))
                .andReturn();
    }

    private MvcResult performLogin(String username) throws Exception {
        return mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + TEST_PASSWORD + "\"}")
                        .header("User-Agent", USER_AGENT))
                .andReturn();
    }

    // =========================================================================
    // Signup — guards métier
    // =========================================================================

    @Nested
    class SignupGuards {

        @Test
        void motsDePasseDifferents_retourne400() throws Exception {
            MvcResult result = mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody("signup_mismatch", TEST_PASSWORD, "AutreMotDePasse123!"))
                            .header("User-Agent", USER_AGENT))
                    .andReturn();

            assertEquals(400, result.getResponse().getStatus());
            assertTrue(userRepository.findByUsernameIgnoreCase("signup_mismatch").isEmpty(),
                    "Aucun utilisateur ne doit être créé si la validation échoue");
        }

        @Test
        void usernameDejaPris_retourne400() throws Exception {
            User existing = new User("signup_taken", "signup_taken@integration-test.com",
                    passwordEncoder.encode(TEST_PASSWORD));
            existing.setEnabled(true);
            existing.setEmailVerified(true);
            existing.setEverActivated(true);
            existing.setActivatedAt(Instant.now());
            userRepository.save(existing);

            MvcResult result = performSignup("signup_taken");

            assertEquals(400, result.getResponse().getStatus());
        }
    }

    // =========================================================================
    // Activation — guards métier
    // =========================================================================

    @Nested
    class ActivationGuards {

        @Test
        void tokenMalFormé_retourne400() throws Exception {
            MvcResult result = performActivate("ceci-n-est-pas-un-token-chiffre");

            assertEquals(400, result.getResponse().getStatus());
        }

        @Test
        void lienReutiliseApresActivation_retourne409() throws Exception {
            User user = signup("activation_reuse");
            String token = activationTokenFor(user);

            assertEquals(200, performActivate(token).getResponse().getStatus());

            // Le token d'activation a été révoqué par confirmNewUserAccount(), mais
            // resend-activation le relit sans vérifier "revoked" — il retombe donc
            // sur le guard "compte déjà activé".
            MvcResult resend = mockMvc.perform(post(RESEND_URL).param("token", token))
                    .andReturn();

            assertEquals(409, resend.getResponse().getStatus());
        }
    }

    // =========================================================================
    // Cycle nominal — signup → activation → login
    // =========================================================================

    @Nested
    class CycleNominal {

        @Test
        void signup_creeUnCompteDesactiveAvecUnTokenDeConfirmation() throws Exception {
            User user = signup("nominal_signup");

            assertFalse(user.isEnabled(), "Le compte doit rester désactivé avant activation");
            assertFalse(user.isEmailVerified());
            assertFalse(user.isEverActivated());

            AccountConfirmationToken token = confirmationTokenRepository
                    .findByUserAndTokenTypeAndRevokedFalse(user, TokenType.ACCOUNT_CONFIRMATION)
                    .orElseThrow();
            assertFalse(token.isRevoked());
            assertNotNull(token.getPublicId());
        }

        @Test
        void activation_activeLeCompteEtRevoqueLeToken() throws Exception {
            User user = signup("nominal_activate");
            String token = activationTokenFor(user);

            mockMvc.perform(get(ACTIVATE_URL).param("token", token))
                    .andExpect(status().isOk());

            User activated = userRepository.findByUsernameIgnoreCase("nominal_activate").orElseThrow();
            assertTrue(activated.isEnabled());
            assertTrue(activated.isEmailVerified());
            assertTrue(activated.isEverActivated());
            assertNotNull(activated.getActivatedAt());

            assertTrue(confirmationTokenRepository
                            .findByUserAndTokenTypeAndRevokedFalse(activated, TokenType.ACCOUNT_CONFIRMATION)
                            .isEmpty(),
                    "Le token doit être révoqué après activation");
        }

        @Test
        void flowComplet_signupActivationLogin() throws Exception {
            String username = "nominal_full_flow";

            User user = signup(username);
            String token = activationTokenFor(user);

            assertEquals(200, performActivate(token).getResponse().getStatus());

            MvcResult loginResult = performLogin(username);
            assertEquals(200, loginResult.getResponse().getStatus(),
                    "Le login doit réussir immédiatement après activation");
        }
    }
}
