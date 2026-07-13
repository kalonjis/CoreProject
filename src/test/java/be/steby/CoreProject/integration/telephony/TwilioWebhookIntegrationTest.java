package be.steby.CoreProject.integration.telephony;

import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dal.repositories.crm.CallLogRepository;
import be.steby.CoreProject.dal.repositories.crm.CallSessionRepository;
import be.steby.CoreProject.dal.repositories.crm.LeadRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CallSession;
import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.dl.enums.crm.CallProvider;
import be.steby.CoreProject.dl.enums.crm.CallSessionStatus;
import be.steby.CoreProject.utils.IntegrationTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the public Twilio endpoints:
 * {@code /api/crm/telephony/twilio/webhook} and {@code /twiml}.
 *
 * <p>Covers the {@code X-Twilio-Signature} verification (HMAC-SHA1 over
 * URL + sorted params, per the Twilio spec) and the call lifecycle
 * transitions driven by status callbacks.
 *
 * <p>Key invariant under test: a request with a missing or invalid
 * signature returns 403 and must NOT touch the call session — every
 * rejection case re-checks the session state in the database.
 *
 * <p>The auth token is fixed in {@code src/test/resources/application.properties}
 * (TWILIO_AUTH_TOKEN) and the request URL is pinned via the {@code Host}
 * header so the signature is deterministic.
 */
class TwilioWebhookIntegrationTest extends IntegrationTestBase {

    private static final String WEBHOOK_URL = "/api/crm/telephony/twilio/webhook";
    private static final String TWIML_URL   = "/api/crm/telephony/twilio/twiml";

    // Must match TWILIO_AUTH_TOKEN in src/test/resources/application.properties
    private static final String AUTH_TOKEN = "test";

    // Pinned via the Host header — buildRequestUrl() reconstructs scheme://host+path
    private static final String HOST = "localhost";

    @Autowired
    private CallSessionRepository callSessionRepository;

    @Autowired
    private CallLogRepository callLogRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private UserRepository userRepository;

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Persists a Twilio call session correlated to {@code callSid}, with its
     * owning commercial and target lead. Identifiers must be unique per test
     * (no rollback between methods).
     */
    private CallSession createTwilioSession(String key, String callSid, CallSessionStatus status) {
        User commercial = userRepository.save(new User(
                "twilio_" + key, "twilio_" + key + "@integration-test.com", "not-used"));

        Lead lead = leadRepository.save(Lead.builder()
                .email("twilio_" + key + "@lead-test.com")
                .subject("Twilio webhook test")
                .leadType(LeadType.GENERAL)
                .submittedAt(Instant.now())
                .build());

        return callSessionRepository.save(CallSession.builder()
                .provider(CallProvider.TWILIO)
                .status(status)
                .phoneNumber("+32470000000")
                .startedAt(Instant.now())
                .lead(lead)
                .performedBy(commercial)
                .externalCallId(callSid)
                .build());
    }

    /**
     * Computes the X-Twilio-Signature as Twilio does: Base64(HMAC-SHA1(url
     * + params concatenated as key+value in alphabetical key order, authToken)).
     */
    private String twilioSignature(String url, Map<String, String> params) throws Exception {
        StringBuilder data = new StringBuilder(url);
        new TreeMap<>(params).forEach((key, value) -> data.append(key).append(value));

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(AUTH_TOKEN.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] hash = mac.doFinal(data.toString().getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    /** Builds a signed (or unsigned when {@code signature} is null) form POST. */
    private MockHttpServletRequestBuilder signedPost(String path, Map<String, String> params, String signature) {
        MockHttpServletRequestBuilder request = post(path)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Host", HOST);
        params.forEach(request::param);
        if (signature != null) {
            request.header("X-Twilio-Signature", signature);
        }
        return request;
    }

    /** POST to the webhook with a signature valid for the given params. */
    private MockHttpServletRequestBuilder validWebhookPost(Map<String, String> params) throws Exception {
        return signedPost(WEBHOOK_URL, params, twilioSignature("http://" + HOST + WEBHOOK_URL, params));
    }

    private CallSession reload(CallSession session) {
        return callSessionRepository.findByPublicId(session.getPublicId()).orElseThrow();
    }

    // =========================================================================
    // Signature invalide — rejet 403, session intacte
    // =========================================================================

    @Nested
    class SignatureInvalide {

        @Test
        void signatureAbsente_retourne403_sansToucherLaSession() throws Exception {
            CallSession session = createTwilioSession("sig_absente", "CA_sig_absente", CallSessionStatus.INITIATED);
            Map<String, String> params = Map.of("CallSid", "CA_sig_absente", "CallStatus", "ringing");

            mockMvc.perform(signedPost(WEBHOOK_URL, params, null))
                    .andExpect(status().isForbidden());

            assertEquals(CallSessionStatus.INITIATED, reload(session).getStatus(),
                    "La session ne doit pas changer sans signature");
        }

        @Test
        void signatureInvalide_retourne403_sansToucherLaSession() throws Exception {
            CallSession session = createTwilioSession("sig_invalide", "CA_sig_invalide", CallSessionStatus.INITIATED);
            Map<String, String> params = Map.of("CallSid", "CA_sig_invalide", "CallStatus", "ringing");

            mockMvc.perform(signedPost(WEBHOOK_URL, params, "fausse-signature-base64"))
                    .andExpect(status().isForbidden());

            assertEquals(CallSessionStatus.INITIATED, reload(session).getStatus(),
                    "La session ne doit pas changer avec une signature invalide");
        }

        @Test
        void signatureValidePourDautresParams_retourne403() throws Exception {
            // Rejeu : signature authentique mais calculée sur un autre payload
            CallSession session = createTwilioSession("sig_rejeu", "CA_sig_rejeu", CallSessionStatus.INITIATED);
            Map<String, String> params      = Map.of("CallSid", "CA_sig_rejeu", "CallStatus", "completed");
            Map<String, String> otherParams = Map.of("CallSid", "CA_autre",     "CallStatus", "ringing");

            String replayedSignature = twilioSignature("http://" + HOST + WEBHOOK_URL, otherParams);

            mockMvc.perform(signedPost(WEBHOOK_URL, params, replayedSignature))
                    .andExpect(status().isForbidden());

            assertEquals(CallSessionStatus.INITIATED, reload(session).getStatus(),
                    "La signature doit couvrir les params exacts de la requête");
        }
    }

    // =========================================================================
    // Cycle de vie — status callbacks signés
    // =========================================================================

    @Nested
    class CycleDeVie {

        @Test
        void ringing_passeLaSessionEnRinging() throws Exception {
            CallSession session = createTwilioSession("ringing", "CA_ringing", CallSessionStatus.INITIATED);

            mockMvc.perform(validWebhookPost(Map.of("CallSid", "CA_ringing", "CallStatus", "ringing")))
                    .andExpect(status().isOk());

            assertEquals(CallSessionStatus.RINGING, reload(session).getStatus());
        }

        @Test
        void inProgress_passeLaSessionEnActive_avecAnsweredAt() throws Exception {
            CallSession session = createTwilioSession("answer", "CA_answer", CallSessionStatus.RINGING);

            mockMvc.perform(validWebhookPost(Map.of("CallSid", "CA_answer", "CallStatus", "in-progress")))
                    .andExpect(status().isOk());

            CallSession updated = reload(session);
            assertEquals(CallSessionStatus.ACTIVE, updated.getStatus());
            assertNotNull(updated.getAnsweredAt(), "answeredAt doit être posé quand l'appel décroche");
        }

        @Test
        void completed_termineLaSession_avecDurationEtCallLog() throws Exception {
            CallSession session = createTwilioSession("completed", "CA_completed", CallSessionStatus.ACTIVE);
            long callLogsBefore = callLogRepository.count();

            mockMvc.perform(validWebhookPost(Map.of(
                            "CallSid", "CA_completed",
                            "CallStatus", "completed",
                            "CallDuration", "42")))
                    .andExpect(status().isOk());

            CallSession updated = reload(session);
            assertEquals(CallSessionStatus.ENDED, updated.getStatus());
            assertEquals(42, updated.getDurationSeconds(), "La durée doit venir du param CallDuration");
            assertNotNull(updated.getEndedAt());
            assertEquals(callLogsBefore + 1, callLogRepository.count(),
                    "Le listener doit créer le CallLog à la terminaison");
        }

        @Test
        void noAnswer_marqueLaSessionMissed() throws Exception {
            CallSession session = createTwilioSession("no_answer", "CA_no_answer", CallSessionStatus.RINGING);

            mockMvc.perform(validWebhookPost(Map.of("CallSid", "CA_no_answer", "CallStatus", "no-answer")))
                    .andExpect(status().isOk());

            CallSession updated = reload(session);
            assertEquals(CallSessionStatus.MISSED, updated.getStatus());
            assertNull(updated.getDurationSeconds(), "Pas de durée pour un appel manqué");
        }

        @Test
        void failed_marqueLaSessionFailed() throws Exception {
            CallSession session = createTwilioSession("failed", "CA_failed", CallSessionStatus.INITIATED);

            mockMvc.perform(validWebhookPost(Map.of("CallSid", "CA_failed", "CallStatus", "failed")))
                    .andExpect(status().isOk());

            assertEquals(CallSessionStatus.FAILED, reload(session).getStatus());
        }
    }

    // =========================================================================
    // Robustesse — payloads signés mais inexploitables
    // =========================================================================

    @Nested
    class Robustesse {

        @Test
        void callSidInconnu_retourne200SansEffet() throws Exception {
            mockMvc.perform(validWebhookPost(Map.of("CallSid", "CA_inconnu_xyz", "CallStatus", "ringing")))
                    .andExpect(status().isOk());
        }

        @Test
        void payloadSansCallSid_retourne200() throws Exception {
            mockMvc.perform(validWebhookPost(Map.of("CallStatus", "ringing")))
                    .andExpect(status().isOk());
        }

        @Test
        void webhookTardif_surSessionDejaTerminee_retourne200SansModif() throws Exception {
            // Le front a déjà terminé la session — le webhook Twilio arrive après
            CallSession session = createTwilioSession("tardif", "CA_tardif", CallSessionStatus.ENDED);
            long callLogsBefore = callLogRepository.count();

            mockMvc.perform(validWebhookPost(Map.of(
                            "CallSid", "CA_tardif",
                            "CallStatus", "completed",
                            "CallDuration", "99")))
                    .andExpect(status().isOk());

            CallSession updated = reload(session);
            assertEquals(CallSessionStatus.ENDED, updated.getStatus());
            assertNull(updated.getDurationSeconds(), "Une session terminée ne doit plus être modifiée");
            assertEquals(callLogsBefore, callLogRepository.count(),
                    "Pas de second CallLog pour un webhook tardif");
        }
    }

    // =========================================================================
    // TwiML — corrélation CallSid et génération du Dial
    // =========================================================================

    @Nested
    class TwiML {

        @Test
        void signatureValide_enregistreLeCallSid_etRetourneLeDial() throws Exception {
            // Session fraîche, pas encore corrélée à Twilio (externalCallId null)
            CallSession session = createTwilioSession("twiml", null, CallSessionStatus.INITIATED);
            Map<String, String> params = Map.of(
                    "CallSid", "CA_twiml_nouveau",
                    "callPublicId", session.getPublicId(),
                    "To", "+32470000000");

            mockMvc.perform(signedPost(TWIML_URL, params,
                            twilioSignature("http://" + HOST + TWIML_URL, params)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_XML))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString(
                            "<Number>+32470000000</Number>")));

            assertEquals("CA_twiml_nouveau", reload(session).getExternalCallId(),
                    "Le CallSid doit être corrélé à la session pour les webhooks suivants");
        }

        @Test
        void signatureInvalide_retourne403_sansCorrelerLeCallSid() throws Exception {
            CallSession session = createTwilioSession("twiml_ko", null, CallSessionStatus.INITIATED);
            Map<String, String> params = Map.of(
                    "CallSid", "CA_twiml_ko",
                    "callPublicId", session.getPublicId(),
                    "To", "+32470000000");

            mockMvc.perform(signedPost(TWIML_URL, params, "fausse-signature"))
                    .andExpect(status().isForbidden());

            assertNull(reload(session).getExternalCallId(),
                    "Le CallSid ne doit pas être enregistré si la signature est invalide");
        }
    }
}
