package be.steby.CoreProject.integration.call;

import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dal.repositories.crm.CallLogRepository;
import be.steby.CoreProject.dal.repositories.crm.CallSessionRepository;
import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dal.repositories.crm.InteractionRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CallLog;
import be.steby.CoreProject.dl.entities.crm.CallSession;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Interaction;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.dl.enums.crm.CallProvider;
import be.steby.CoreProject.dl.enums.crm.CallSessionStatus;
import be.steby.CoreProject.dl.enums.crm.CallStatus;
import be.steby.CoreProject.dl.enums.crm.InteractionOutcome;
import be.steby.CoreProject.dl.enums.crm.InteractionType;
import be.steby.CoreProject.utils.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the call session lifecycle over REST:
 * {@code POST /api/crm/calls} (initiate) and
 * {@code PATCH /api/crm/calls/{publicId}/terminate}.
 *
 * <p>Runs with the TEL_URI provider (default in test config) — no network,
 * no Twilio, no Asterisk. Covers:
 * <ol>
 *   <li>Access control (COMMERCIAL/ADMIN required, CSRF on state-changing verbs)</li>
 *   <li>Initiate guards — contact or lead required (outbound), single active session</li>
 *   <li>Terminate guards — unknown session, non-terminal status, already terminated</li>
 *   <li>Nominal flow — session persisted, then {@code CallTerminatedInteractionListener}
 *       creates the {@link Interaction} + {@link CallLog}</li>
 * </ol>
 *
 * <p>Each test that initiates a call uses a dedicated commercial: the
 * single-active-session guard is per-user and there is no rollback between methods.
 */
class CallLifecycleIntegrationTest extends IntegrationTestBase {

    private static final String CALLS_URL     = "/api/crm/calls";
    private static final String LOGIN_URL     = "/api/auth/login";
    private static final String TEST_PASSWORD = "TestPass123!";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private CallSessionRepository callSessionRepository;

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private CallLogRepository callLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Authenticated session — access token + CSRF token, both required for POST/PATCH. */
    private record AuthSession(Cookie accessToken, Cookie xsrfToken) {}

    /** Persists a fully-activated user that can log in without 2FA. */
    private User createActiveUser(String username, UserRole topRole) {
        User user = new User(username, username + "@integration-test.com",
                passwordEncoder.encode(TEST_PASSWORD));
        user.setUserRoles(UserRole.setRoles(topRole));
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setEverActivated(true);
        user.setTwoFactorEnabled(false);
        user.setMustChangePassword(false);
        user.setActivatedAt(Instant.now());
        return userRepository.save(user);
    }

    /**
     * Creates a dedicated commercial and logs it in.
     * One commercial per test: the active-session guard is per-user.
     */
    private AuthSession loginAsNewCommercial(String key) throws Exception {
        String username = "calllifecycle_" + key;
        createActiveUser(username, UserRole.COMMERCIAL);

        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + TEST_PASSWORD + "\"}")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie access = extractCookie(result, "access_token");
        Cookie xsrf   = extractCookie(result, "XSRF-TOKEN");
        assertNotNull(access, "access_token cookie not found after login");
        assertNotNull(xsrf,   "XSRF-TOKEN cookie not found after login");
        return new AuthSession(access, xsrf);
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

    private Contact createContact(String key, String phone) {
        return contactRepository.save(Contact.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .email("calllifecycle_" + key + "@contact-test.com")
                .phone(phone)
                .build());
    }

    /** POSTs an initiate request and returns the raw MvcResult (no status assertion). */
    private MvcResult performInitiate(AuthSession auth, String body) throws Exception {
        return mockMvc.perform(post(CALLS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(auth.accessToken(), auth.xsrfToken())
                        .header("X-XSRF-TOKEN", auth.xsrfToken().getValue()))
                .andReturn();
    }

    /** Initiates an outbound call towards the given contact and expects 201. Returns the session publicId. */
    private String initiateCall(AuthSession auth, Contact contact) throws Exception {
        MvcResult result = mockMvc.perform(post(CALLS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + contact.getPhone() + "\","
                                + "\"contactPublicId\":\"" + contact.getPublicId() + "\"}")
                        .cookie(auth.accessToken(), auth.xsrfToken())
                        .header("X-XSRF-TOKEN", auth.xsrfToken().getValue()))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("publicId").asText();
    }

    /** PATCHes the terminate endpoint and returns the raw MvcResult (no status assertion). */
    private MvcResult performTerminate(AuthSession auth, String publicId, String body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.patch(CALLS_URL + "/" + publicId + "/terminate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(auth.accessToken(), auth.xsrfToken())
                        .header("X-XSRF-TOKEN", auth.xsrfToken().getValue()))
                .andReturn();
    }

    /** PATCHes the answer endpoint and returns the raw MvcResult (no status assertion). */
    private MvcResult performAnswer(AuthSession auth, String publicId) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.patch(CALLS_URL + "/" + publicId + "/answer")
                        .cookie(auth.accessToken(), auth.xsrfToken())
                        .header("X-XSRF-TOKEN", auth.xsrfToken().getValue()))
                .andReturn();
    }

    // =========================================================================
    // Contrôle d'accès
    // =========================================================================

    @Nested
    class ControleDacces {

        @Test
        void sansAuthentification_retourne401() throws Exception {
            mockMvc.perform(get(CALLS_URL + "/session-inexistante"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void roleUserSimple_retourne403() throws Exception {
            createActiveUser("calllifecycle_user_simple", UserRole.USER);

            MvcResult login = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"calllifecycle_user_simple\",\"password\":\"" + TEST_PASSWORD + "\"}")
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0"))
                    .andExpect(status().isOk())
                    .andReturn();

            mockMvc.perform(get(CALLS_URL + "/session-inexistante")
                            .cookie(extractCookie(login, "access_token")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void answerSansAuthentificationNiCsrf_retourne403() throws Exception {
            // PATCH sans cookie XSRF-TOKEN : le CsrfFilter rejette avant même
            // que l'authentification soit évaluée (cf. sansAuthentification_retourne401
            // ci-dessus, sur GET, qui n'est pas soumis au CSRF).
            mockMvc.perform(MockMvcRequestBuilders.patch(CALLS_URL + "/session-inexistante/answer"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void answerRoleUserSimple_retourne403() throws Exception {
            createActiveUser("calllifecycle_answer_user", UserRole.USER);

            MvcResult login = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"calllifecycle_answer_user\",\"password\":\"" + TEST_PASSWORD + "\"}")
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0"))
                    .andExpect(status().isOk())
                    .andReturn();

            Cookie access = extractCookie(login, "access_token");
            Cookie xsrf   = extractCookie(login, "XSRF-TOKEN");

            // Cookie + header CSRF fournis pour isoler le 403 de @PreAuthorize
            // (sans eux, le CsrfFilter renverrait 403 avant d'atteindre l'autorisation).
            mockMvc.perform(MockMvcRequestBuilders.patch(CALLS_URL + "/session-inexistante/answer")
                            .cookie(access, xsrf)
                            .header("X-XSRF-TOKEN", xsrf.getValue()))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // Initiate — guards métier
    // =========================================================================

    @Nested
    class InitiateGuards {

        @Test
        void outboundSansContactNiLead_retourne400() throws Exception {
            AuthSession auth = loginAsNewCommercial("no_target");

            MvcResult result = performInitiate(auth, "{\"phoneNumber\":\"+32470000001\"}");

            assertEquals(400, result.getResponse().getStatus());
        }

        @Test
        void contactInexistant_retourne404() throws Exception {
            AuthSession auth = loginAsNewCommercial("ghost_contact");

            MvcResult result = performInitiate(auth,
                    "{\"phoneNumber\":\"+32470000002\",\"contactPublicId\":\"contact-inexistant\"}");

            assertEquals(404, result.getResponse().getStatus());
        }

        @Test
        void doubleSessionActive_retourne409() throws Exception {
            AuthSession auth = loginAsNewCommercial("double");
            Contact contact = createContact("double", "+32470000003");

            initiateCall(auth, contact);

            MvcResult second = performInitiate(auth,
                    "{\"phoneNumber\":\"" + contact.getPhone() + "\","
                            + "\"contactPublicId\":\"" + contact.getPublicId() + "\"}");

            assertEquals(409, second.getResponse().getStatus());
        }
    }

    // =========================================================================
    // Terminate — guards métier
    // =========================================================================

    @Nested
    class TerminateGuards {

        @Test
        void sessionInexistante_retourne404() throws Exception {
            AuthSession auth = loginAsNewCommercial("term_ghost");

            MvcResult result = performTerminate(auth, "session-inexistante",
                    "{\"status\":\"ENDED\",\"durationSeconds\":60}");

            assertEquals(404, result.getResponse().getStatus());
        }

        @Test
        void statusNonTerminal_retourne400() throws Exception {
            AuthSession auth = loginAsNewCommercial("term_active");
            Contact contact = createContact("term_active", "+32470000004");
            String publicId = initiateCall(auth, contact);

            MvcResult result = performTerminate(auth, publicId, "{\"status\":\"ACTIVE\"}");

            assertEquals(400, result.getResponse().getStatus());
        }

        @Test
        void sessionDejaTerminee_retourne400() throws Exception {
            AuthSession auth = loginAsNewCommercial("term_twice");
            Contact contact = createContact("term_twice", "+32470000005");
            String publicId = initiateCall(auth, contact);

            assertEquals(200, performTerminate(auth, publicId,
                    "{\"status\":\"ENDED\",\"durationSeconds\":30}").getResponse().getStatus());

            MvcResult second = performTerminate(auth, publicId,
                    "{\"status\":\"ENDED\",\"durationSeconds\":30}");

            assertEquals(400, second.getResponse().getStatus());
        }
    }

    // =========================================================================
    // Answer — guards métier
    // =========================================================================

    @Nested
    class AnswerGuards {

        @Test
        void dejaActive_retourne400() throws Exception {
            AuthSession auth = loginAsNewCommercial("answer_twice");
            Contact contact = createContact("answer_twice", "+32470000010");
            String publicId = initiateCall(auth, contact);

            assertEquals(200, performAnswer(auth, publicId).getResponse().getStatus());

            MvcResult second = performAnswer(auth, publicId);
            assertEquals(400, second.getResponse().getStatus());
        }

        @Test
        void sessionDejaTerminee_retourne400() throws Exception {
            AuthSession auth = loginAsNewCommercial("answer_ended");
            Contact contact = createContact("answer_ended", "+32470000011");
            String publicId = initiateCall(auth, contact);

            assertEquals(200, performTerminate(auth, publicId,
                    "{\"status\":\"ENDED\",\"durationSeconds\":30}").getResponse().getStatus());

            MvcResult result = performAnswer(auth, publicId);
            assertEquals(400, result.getResponse().getStatus());
        }
    }

    // =========================================================================
    // Cycle nominal — initiate → terminate → Interaction + CallLog
    // =========================================================================

    @Nested
    class CycleNominal {

        @Test
        void initiate_creeUneSessionTelUriEnBase() throws Exception {
            AuthSession auth = loginAsNewCommercial("nominal_init");
            Contact contact = createContact("nominal_init", "+32470000006");

            MvcResult result = mockMvc.perform(post(CALLS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phoneNumber\":\"" + contact.getPhone() + "\","
                                    + "\"contactPublicId\":\"" + contact.getPublicId() + "\"}")
                            .cookie(auth.accessToken(), auth.xsrfToken())
                            .header("X-XSRF-TOKEN", auth.xsrfToken().getValue()))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.status").value("INITIATED"))
                    .andExpect(jsonPath("$.provider").value("TEL_URI"))
                    .andExpect(jsonPath("$.phoneNumber").value(contact.getPhone()))
                    .andExpect(jsonPath("$.contactPublicId").value(contact.getPublicId()))
                    .andExpect(jsonPath("$.endedAt").isEmpty())
                    .andReturn();

            String publicId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("publicId").asText();

            CallSession session = callSessionRepository.findByPublicId(publicId).orElseThrow();
            assertEquals(CallSessionStatus.INITIATED, session.getStatus());
            assertEquals(CallProvider.TEL_URI, session.getProvider());
            assertNotNull(session.getStartedAt());
        }

        @Test
        void answer_passeLaSessionEnActiveEtDefinitAnsweredAt() throws Exception {
            AuthSession auth = loginAsNewCommercial("nominal_answer");
            Contact contact = createContact("nominal_answer", "+32470000012");
            String publicId = initiateCall(auth, contact);

            MvcResult result = performAnswer(auth, publicId);
            assertEquals(200, result.getResponse().getStatus());

            JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
            assertEquals("ACTIVE", json.get("status").asText());
            assertFalse(json.get("answeredAt").isNull());

            CallSession session = callSessionRepository.findByPublicId(publicId).orElseThrow();
            assertEquals(CallSessionStatus.ACTIVE, session.getStatus());
            assertNotNull(session.getAnsweredAt());
        }

        @Test
        void terminateEnded_metAJourLaSessionEtCreeInteractionEtCallLog() throws Exception {
            AuthSession auth = loginAsNewCommercial("nominal_end");
            Contact contact = createContact("nominal_end", "+32470000007");
            String publicId = initiateCall(auth, contact);

            MvcResult result = performTerminate(auth, publicId,
                    "{\"status\":\"ENDED\",\"durationSeconds\":120}");
            assertEquals(200, result.getResponse().getStatus());

            // Session terminée en base
            CallSession session = callSessionRepository.findByPublicId(publicId).orElseThrow();
            assertEquals(CallSessionStatus.ENDED, session.getStatus());
            assertNotNull(session.getEndedAt());
            assertEquals(120, session.getDurationSeconds());

            // Interaction créée par le listener synchrone
            List<Interaction> interactions =
                    interactionRepository.findByContactIdOrderByOccurredAtDesc(contact.getId());
            assertEquals(1, interactions.size(), "Le listener doit créer exactement une Interaction");

            Interaction interaction = interactions.getFirst();
            assertEquals(InteractionType.CALL, interaction.getType());
            assertEquals(InteractionOutcome.NEUTRAL, interaction.getOutcome());
            assertEquals(2, interaction.getDurationMinutes());

            // CallLog rattaché à l'interaction
            CallLog callLog = callLogRepository.findByInteractionId(interaction.getId()).orElseThrow();
            assertEquals(CallStatus.ANSWERED, callLog.getStatus());
            assertEquals(contact.getPhone(), callLog.getPhoneNumber());
            assertEquals(120, callLog.getDurationSeconds());
        }

        @Test
        void terminateMissed_creeUnCallLogNoAnswerSansDuree() throws Exception {
            AuthSession auth = loginAsNewCommercial("nominal_missed");
            Contact contact = createContact("nominal_missed", "+32470000008");
            String publicId = initiateCall(auth, contact);

            MvcResult result = performTerminate(auth, publicId, "{\"status\":\"MISSED\"}");
            assertEquals(200, result.getResponse().getStatus());

            CallSession session = callSessionRepository.findByPublicId(publicId).orElseThrow();
            assertEquals(CallSessionStatus.MISSED, session.getStatus());

            List<Interaction> interactions =
                    interactionRepository.findByContactIdOrderByOccurredAtDesc(contact.getId());
            assertEquals(1, interactions.size());

            Interaction interaction = interactions.getFirst();
            assertEquals(InteractionOutcome.NO_ANSWER, interaction.getOutcome());
            assertNull(interaction.getDurationMinutes(), "Pas de durée pour un appel manqué");

            CallLog callLog = callLogRepository.findByInteractionId(interaction.getId()).orElseThrow();
            assertEquals(CallStatus.NO_ANSWER, callLog.getStatus());
        }

        @Test
        void inboundSansContact_terminateSansCreerDinteraction() throws Exception {
            AuthSession auth = loginAsNewCommercial("inbound_unknown");

            // INBOUND : le contact peut être inconnu au décroché — la validation est ignorée
            MvcResult initiate = performInitiate(auth,
                    "{\"phoneNumber\":\"+32499000009\",\"direction\":\"INBOUND\"}");
            assertEquals(201, initiate.getResponse().getStatus());

            String publicId = objectMapper.readTree(initiate.getResponse().getContentAsString())
                    .get("publicId").asText();

            long callLogsBefore = callLogRepository.count();

            MvcResult result = performTerminate(auth, publicId,
                    "{\"status\":\"ENDED\",\"durationSeconds\":45}");
            assertEquals(200, result.getResponse().getStatus());

            // Session bien terminée, mais aucune interaction créée (appelant inconnu)
            CallSession session = callSessionRepository.findByPublicId(publicId).orElseThrow();
            assertEquals(CallSessionStatus.ENDED, session.getStatus());
            assertEquals(callLogsBefore, callLogRepository.count(),
                    "Aucun CallLog ne doit être créé pour un appel entrant sans contact ni lead");
        }
    }
}
