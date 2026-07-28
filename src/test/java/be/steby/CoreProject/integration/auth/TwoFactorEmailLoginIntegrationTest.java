package be.steby.CoreProject.integration.auth;

import be.steby.CoreProject.bll.domains.auth.services.notifications.email.AuthMailerService;
import be.steby.CoreProject.bll.domains.auth.services.twofactor.emailtwofactor.EmailTwoFactorService;
import be.steby.CoreProject.dal.repositories.TwoFactorAuthRepository;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dl.entities.TwoFactorAuth;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import be.steby.CoreProject.utils.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the EMAIL 2FA login flow:
 * initiate-login → choose-method(EMAIL) → verify-2fa.
 *
 * <p>Unlike password reset, EMAIL 2FA is stateless: the verification code's
 * hash rides inside the signed {@code 2fa_token} JWT cookie
 * ({@code AuthServiceImpl#chooseTwoFactorMethod}) — the plain code is never
 * persisted, it only ever exists in memory and in the outgoing email. To
 * exercise the success path, {@link EmailTwoFactorService#generateVerificationCode}
 * is wrapped with a {@link MockitoSpyBean} that calls through to the real
 * implementation and additionally captures the plain code so the test can
 * submit it back to {@code verify-2fa}.
 *
 * <p>A second {@link MockitoSpyBean} no-ops {@link AuthMailerService#sendTwoFactorCodeSync}:
 * {@code spring.mail.host} isn't populated during {@code mvnw test} (only
 * IntelliJ's EnvFile plugin loads {@code .env}, Maven Surefire doesn't), so
 * it falls back to Spring Boot's {@code localhost:25} default and the real
 * SMTP send fails synchronously — which the (non-{@code @Async}) choose-method
 * endpoint turns into a real 503. Neutralizing the SMTP transport for this
 * one call is the same category of trade-off as H2 replacing PostgreSQL or
 * rate limiting being disabled for the {@code integration} profile — it
 * doesn't touch the business logic under test (code generation, hashing,
 * JWT, guards).
 *
 * <p>This is the only integration test in the suite that uses Mockito —
 * every other flow relies purely on MockMvc against real beans.
 *
 * <p>Covers:
 * <ol>
 *   <li>Initiate-login — 2FA required vs. immediate login, bad credentials</li>
 *   <li>Choose-method guards — disabled method</li>
 *   <li>Verify guards — missing token, wrong code</li>
 *   <li>Nominal flow — full round trip with the real generated code</li>
 * </ol>
 */
class TwoFactorEmailLoginIntegrationTest extends IntegrationTestBase {

    private static final String INITIATE_LOGIN_URL = "/api/auth/initiate-login";
    private static final String CHOOSE_METHOD_URL   = "/api/auth/2fa/choose-method";
    private static final String VERIFY_2FA_URL      = "/api/auth/verify-2fa";
    private static final String ME_URL              = "/api/auth/me";
    private static final String TEST_PASSWORD = "TestPass123!";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TwoFactorAuthRepository twoFactorAuthRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoSpyBean
    private EmailTwoFactorService emailTwoFactorService;

    @MockitoSpyBean
    private AuthMailerService authMailerService;

    private final AtomicReference<String> capturedEmailCode = new AtomicReference<>();

    /**
     * Captures the plain code generated for EMAIL 2FA without altering behavior,
     * and no-ops the actual SMTP send (see class Javadoc — the real relay isn't
     * reachable from {@code mvnw test}).
     */
    @BeforeEach
    void captureGeneratedEmailCodes() {
        Mockito.doAnswer(invocation -> {
            String code = (String) invocation.callRealMethod();
            capturedEmailCode.set(code);
            return code;
        }).when(emailTwoFactorService).generateVerificationCode(Mockito.any());

        Mockito.doNothing().when(authMailerService)
                .sendTwoFactorCodeSync(Mockito.any(), Mockito.any(), Mockito.any());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

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

    /** Persists a user with EMAIL 2FA enabled and set as primary. */
    private User createUserWithEmailTwoFactor(String username) {
        User user = createActiveUser(username);
        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        twoFactorAuthRepository.save(TwoFactorAuth.builder()
                .user(user)
                .type(TwoFactorType.EMAIL)
                .enabled(true)
                .isPrimary(true)
                .enabledAt(Instant.now())
                .build());

        return user;
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

    private MvcResult performInitiateLogin(String username, String password) throws Exception {
        return mockMvc.perform(post(INITIATE_LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")
                        .header("User-Agent", USER_AGENT))
                .andReturn();
    }

    private MvcResult performChooseMethod(Cookie sessionToken, String twoFactorType) throws Exception {
        var request = post(CHOOSE_METHOD_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"twoFactorType\":\"" + twoFactorType + "\"}")
                .header("User-Agent", USER_AGENT);
        if (sessionToken != null) {
            request.cookie(sessionToken);
        }
        return mockMvc.perform(request).andReturn();
    }

    private MvcResult performVerify2fa(Cookie twoFactorToken, String code) throws Exception {
        var request = post(VERIFY_2FA_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"verificationCode\":\"" + code + "\"}")
                .header("User-Agent", USER_AGENT);
        if (twoFactorToken != null) {
            request.cookie(twoFactorToken);
        }
        return mockMvc.perform(request).andReturn();
    }

    /** Runs initiate-login + choose-method(EMAIL) for a 2FA-enabled user and returns the 2fa_token cookie. */
    private Cookie beginEmailTwoFactorChallenge(String username) throws Exception {
        MvcResult initiate = performInitiateLogin(username, TEST_PASSWORD);
        assertEquals(200, initiate.getResponse().getStatus());
        Cookie sessionToken = extractCookie(initiate, "2fa_session_token");
        assertNotNull(sessionToken);

        MvcResult chooseMethod = performChooseMethod(sessionToken, "EMAIL");
        assertEquals(200, chooseMethod.getResponse().getStatus());
        Cookie twoFactorToken = extractCookie(chooseMethod, "2fa_token");
        assertNotNull(twoFactorToken);

        return twoFactorToken;
    }

    // =========================================================================
    // Initiate-login
    // =========================================================================

    @Nested
    class InitiateLogin {

        @Test
        void sansTwoFA_completeImmediatementAvecCookiesAuth() throws Exception {
            createActiveUser("tfa_no2fa");

            MvcResult result = performInitiateLogin("tfa_no2fa", TEST_PASSWORD);

            assertEquals(200, result.getResponse().getStatus());
            assertNotNull(extractCookie(result, "access_token"),
                    "Sans 2FA, initiate-login doit compléter le login immédiatement");
            assertNull(extractCookie(result, "2fa_session_token"));
        }

        @Test
        void avecEmailTwoFA_retourne200EtCookieSessionSansAuthCookies() throws Exception {
            createUserWithEmailTwoFactor("tfa_required");

            MvcResult result = performInitiateLogin("tfa_required", TEST_PASSWORD);

            assertEquals(200, result.getResponse().getStatus());
            assertNotNull(extractCookie(result, "2fa_session_token"),
                    "Le 2FA étant requis, un cookie de session 2FA doit être posé");
            assertNull(extractCookie(result, "access_token"),
                    "Le login ne doit pas être complété avant vérification du 2FA");
        }

        @Test
        void motDePasseErrone_retourne401() throws Exception {
            createUserWithEmailTwoFactor("tfa_bad_pwd");

            MvcResult result = performInitiateLogin("tfa_bad_pwd", "MauvaisMotDePasse!");

            assertEquals(401, result.getResponse().getStatus());
        }
    }

    // =========================================================================
    // Choose-method — guards métier
    // =========================================================================

    @Nested
    class ChooseMethodGuards {

        @Test
        void methodeNonActivee_retourne404() throws Exception {
            createUserWithEmailTwoFactor("tfa_wrong_method");
            MvcResult initiate = performInitiateLogin("tfa_wrong_method", TEST_PASSWORD);
            Cookie sessionToken = extractCookie(initiate, "2fa_session_token");

            // Seul EMAIL est activé pour cet utilisateur — SMS doit être refusé.
            MvcResult result = performChooseMethod(sessionToken, "SMS");

            assertEquals(404, result.getResponse().getStatus());
        }
    }

    // =========================================================================
    // Verify-2fa — guards métier
    // =========================================================================

    @Nested
    class VerifyGuards {

        @Test
        void sansToken2fa_retourne401() throws Exception {
            MvcResult result = performVerify2fa(null, "123456");

            assertEquals(401, result.getResponse().getStatus());
        }

        @Test
        void codeIncorrect_retourne401() throws Exception {
            createUserWithEmailTwoFactor("tfa_wrong_code");
            Cookie twoFactorToken = beginEmailTwoFactorChallenge("tfa_wrong_code");

            MvcResult result = performVerify2fa(twoFactorToken, "000000");

            assertEquals(401, result.getResponse().getStatus());
        }
    }

    // =========================================================================
    // Cycle nominal — initiate-login → choose-method → verify-2fa
    // =========================================================================

    @Nested
    class CycleNominal {

        @Test
        void flowComplet_loginAvecLeVraiCodeEnvoyeParEmail() throws Exception {
            createUserWithEmailTwoFactor("tfa_full_flow");

            Cookie twoFactorToken = beginEmailTwoFactorChallenge("tfa_full_flow");
            String code = capturedEmailCode.get();
            assertNotNull(code, "Le code généré et envoyé par email doit avoir été capturé par le spy");

            MvcResult verify = performVerify2fa(twoFactorToken, code);

            assertEquals(200, verify.getResponse().getStatus());
            Cookie accessToken = extractCookie(verify, "access_token");
            assertNotNull(accessToken, "La vérification réussie doit compléter le login");
            assertNotNull(extractCookie(verify, "refresh_token"));

            mockMvc.perform(get(ME_URL).cookie(accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("tfa_full_flow"));
        }
    }
}
