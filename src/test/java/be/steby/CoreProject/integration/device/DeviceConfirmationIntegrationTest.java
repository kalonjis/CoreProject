package be.steby.CoreProject.integration.device;

import be.steby.CoreProject.bll.common.services.tokens.SecureTokenService;
import be.steby.CoreProject.bll.domains.device.services.tokens.confirmation.DeviceConfirmationTokenServiceImpl;
import be.steby.CoreProject.dal.repositories.DeviceRepository;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dal.repositories.tokens.DeviceTokenRepository;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import be.steby.CoreProject.utils.IntegrationTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the device confirmation flow:
 * login from an unrecognized device → confirm/reject email link.
 *
 * <p>Runs with the full Spring context (H2, real filters). Every login from an
 * unconfirmed device publishes a {@code DeviceSecurityEvent}, handled
 * {@code @Async} by {@code DeviceNotificationListener}, which is what actually
 * creates the {@code DeviceConfirmationToken} and (attempts to) send the email.
 * Calling {@code createDeviceConfirmationToken} a second time from the test
 * thread right after login races that listener — both inserts hit the same
 * {@code UserAttempt} unique constraint (user, type, device), which is flaky
 * under load (only surfaces when the full suite runs, not this file alone).
 * Tests therefore poll {@link DeviceTokenRepository} for the token the
 * listener already created ({@link #awaitConfirmationToken}) instead of
 * minting a competing one. No mocking involved — SMTP failures are swallowed
 * by the listener's own try/catch, so the (real, un-awaited) email send
 * doesn't affect the HTTP response either way.
 *
 * <p>Covers:
 * <ol>
 *   <li>Confirm guards — malformed token, unknown token, reuse after confirmation</li>
 *   <li>Reject guards — malformed token, reuse after rejection</li>
 *   <li>Nominal flow — confirmation trusts the device, rejection blacklists it
 *       and blocks the next login attempt</li>
 * </ol>
 */
class DeviceConfirmationIntegrationTest extends IntegrationTestBase {

    private static final String CONFIRM_URL = "/api/device/confirm";
    private static final String REJECT_URL  = "/api/device/reject";
    private static final String LOGIN_URL   = "/api/auth/login";
    private static final String TEST_PASSWORD = "TestPass123!";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private DeviceConfirmationTokenServiceImpl deviceConfirmationTokenService;

    @Autowired
    private SecureTokenService secureTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Persists an active user whose activation is old enough that a first login won't auto-confirm its device. */
    private User createActiveUser(String username) {
        User user = new User(username, username + "@integration-test.com",
                passwordEncoder.encode(TEST_PASSWORD));
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setEverActivated(true);
        user.setMustChangePassword(false);
        user.setActivatedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        return userRepository.save(user);
    }

    private MvcResult performLogin(String username, String userAgent) throws Exception {
        return mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + TEST_PASSWORD + "\"}")
                        .header("User-Agent", userAgent))
                .andReturn();
    }

    /** Logs the user in once and returns the single device this created. */
    private Device loginAndGetDevice(String username, String userAgent) throws Exception {
        assertEquals(200, performLogin(username, userAgent).getResponse().getStatus());

        User user = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        List<Device> devices = deviceRepository.findAllByUser(user);
        assertEquals(1, devices.size(), "Le premier login doit créer un unique appareil");
        return devices.get(0);
    }

    /** Creates a real DEVICE_CONFIRMATION token for the given device — only safe when no login just fired the async listener for the same device (see class Javadoc). */
    private String confirmationTokenFor(User user, Device device) {
        return deviceConfirmationTokenService
                .createDeviceConfirmationToken(user, device.getId())
                .getPublicId();
    }

    /** Polls for the DEVICE_CONFIRMATION token {@code DeviceNotificationListener} creates asynchronously right after login. */
    private String awaitConfirmationToken(User user) throws InterruptedException {
        for (int attempt = 0; attempt < 50; attempt++) {
            var token = deviceTokenRepository.findByUserAndTokenTypeAndRevokedFalse(user, TokenType.DEVICE_CONFIRMATION);
            if (token.isPresent()) {
                return token.get().getPublicId();
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("DeviceNotificationListener never created a confirmation token for user " + user.getUsername());
    }

    private MvcResult performConfirm(String token) throws Exception {
        return mockMvc.perform(get(CONFIRM_URL).param("token", token).header("User-Agent", USER_AGENT))
                .andReturn();
    }

    private MvcResult performReject(String token) throws Exception {
        return mockMvc.perform(get(REJECT_URL).param("token", token).header("User-Agent", USER_AGENT))
                .andReturn();
    }

    private Device reload(Device device) {
        return deviceRepository.findById(device.getId()).orElseThrow();
    }

    // =========================================================================
    // Confirm — guards métier
    // =========================================================================

    @Nested
    class ConfirmGuards {

        @Test
        void tokenMalFormé_retourne400() throws Exception {
            MvcResult result = performConfirm("ceci-n-est-pas-un-token-chiffre");

            assertEquals(400, result.getResponse().getStatus());
        }

        @Test
        void tokenInexistant_retourne404() throws Exception {
            String unknownToken = secureTokenService.secureToken("un-token-qui-n-existe-pas-en-base");

            MvcResult result = performConfirm(unknownToken);

            assertEquals(404, result.getResponse().getStatus());
        }

        @Test
        void lienReutiliseApresConfirmation_retourne404() throws Exception {
            User user = createActiveUser("devconf_reuse_confirm");
            loginAndGetDevice("devconf_reuse_confirm", USER_AGENT);
            String token = awaitConfirmationToken(user);

            assertEquals(200, performConfirm(token).getResponse().getStatus());

            // Le token a été révoqué par la première confirmation — le rejouer
            // retombe sur "introuvable" (getSecureValidToken ne filtre que revoked=false).
            MvcResult replay = performConfirm(token);

            assertEquals(404, replay.getResponse().getStatus());
        }
    }

    // =========================================================================
    // Reject — guards métier
    // =========================================================================

    @Nested
    class RejectGuards {

        @Test
        void tokenMalFormé_retourne400() throws Exception {
            MvcResult result = performReject("ceci-n-est-pas-un-token-chiffre");

            assertEquals(400, result.getResponse().getStatus());
        }

        @Test
        void lienReutiliseApresRejet_retourne404() throws Exception {
            User user = createActiveUser("devconf_reuse_reject");
            loginAndGetDevice("devconf_reuse_reject", USER_AGENT);
            String token = awaitConfirmationToken(user);

            assertEquals(200, performReject(token).getResponse().getStatus());

            MvcResult replay = performReject(token);

            assertEquals(404, replay.getResponse().getStatus());
        }
    }

    // =========================================================================
    // Cycle nominal — login (appareil non confirmé) → confirm / reject
    // =========================================================================

    @Nested
    class CycleNominal {

        @Test
        void login_creeUnAppareilNonConfirmeEtNonFaitConfiance() throws Exception {
            createActiveUser("devconf_nominal_login");

            Device device = loginAndGetDevice("devconf_nominal_login", USER_AGENT);

            assertFalse(device.isConfirmed());
            assertEquals(DeviceTrustLevel.UNTRUSTED, device.getDeviceTrustLevel());
            assertFalse(device.isBlacklisted());
        }

        @Test
        void confirmation_faitConfianceALAppareilEtRevoqueLeToken() throws Exception {
            User user = createActiveUser("devconf_nominal_confirm");
            Device device = loginAndGetDevice("devconf_nominal_confirm", USER_AGENT);
            String token = awaitConfirmationToken(user);

            mockMvc.perform(get(CONFIRM_URL).param("token", token).header("User-Agent", USER_AGENT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.operation").value("DEVICE_CONFIRMED"))
                    .andExpect(jsonPath("$.data").value(device.getPublicId()));

            Device confirmed = reload(device);
            assertTrue(confirmed.isConfirmed());
            assertEquals(DeviceTrustLevel.TRUSTED, confirmed.getDeviceTrustLevel());

            assertTrue(deviceTokenRepository
                            .findByUserAndTokenTypeAndRevokedFalse(user, TokenType.DEVICE_CONFIRMATION)
                            .isEmpty(),
                    "Le token doit être révoqué après confirmation");
        }

        @Test
        void confirmation_reactiveUnAppareilPrealablementRejete() throws Exception {
            User user = createActiveUser("devconf_reactivate");
            Device device = loginAndGetDevice("devconf_reactivate", USER_AGENT);

            String rejectToken = awaitConfirmationToken(user);
            assertEquals(200, performReject(rejectToken).getResponse().getStatus());
            assertTrue(reload(device).isBlacklisted());

            // Nouveau lien de confirmation pour le même appareil (celui du rejet a été révoqué,
            // et aucun login concurrent n'est en cours — pas de risque de course ici).
            String confirmToken = confirmationTokenFor(user, device);
            assertEquals(200, performConfirm(confirmToken).getResponse().getStatus());

            Device confirmed = reload(device);
            assertTrue(confirmed.isConfirmed());
            assertFalse(confirmed.isBlacklisted(), "La confirmation doit lever le blacklist");
            assertEquals(DeviceTrustLevel.TRUSTED, confirmed.getDeviceTrustLevel());
        }

        @Test
        void rejet_blackListeLAppareilEtDeconnecte() throws Exception {
            User user = createActiveUser("devconf_nominal_reject");
            Device device = loginAndGetDevice("devconf_nominal_reject", USER_AGENT);
            String token = awaitConfirmationToken(user);

            mockMvc.perform(get(REJECT_URL).param("token", token).header("User-Agent", USER_AGENT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.operation").value("DEVICE_REJECTED"));

            Device rejected = reload(device);
            assertFalse(rejected.isConfirmed());
            assertEquals(DeviceTrustLevel.UNTRUSTED, rejected.getDeviceTrustLevel());
            assertTrue(rejected.isBlacklisted());
            assertNotNull(rejected.getBlacklistedTime());
            assertTrue(rejected.isLoggedOut());
            assertNotNull(rejected.getLogoutTime());
        }

        @Test
        void rejet_bloqueLeProchainLoginDepuisCetAppareil() throws Exception {
            User user = createActiveUser("devconf_reject_blocks_login");
            loginAndGetDevice("devconf_reject_blocks_login", USER_AGENT);
            String token = awaitConfirmationToken(user);

            assertEquals(200, performReject(token).getResponse().getStatus());

            // Même User-Agent → même fingerprint → même appareil, désormais blacklisté.
            MvcResult secondLogin = performLogin("devconf_reject_blocks_login", USER_AGENT);

            assertEquals(403, secondLogin.getResponse().getStatus());
        }
    }
}
