package be.steby.CoreProject.integration.lead;

import be.steby.CoreProject.dal.repositories.crm.LeadRepository;
import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.dl.enums.crm.LeadSource;
import be.steby.CoreProject.utils.IntegrationTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the public lead ingest webhook:
 * {@code /api/public/leads/ingest/{source}}.
 *
 * <p>Covers the two verification algorithms (PLAIN_TOKEN via the "generic"
 * source, HMAC-SHA256 via the "typeform" source) and the Facebook challenge
 * handshake.
 *
 * <p>Key invariant under test: a request with a missing or invalid signature
 * returns 200 (deliberately silent, to leak nothing to attackers) but must
 * NOT create a lead. Asserting on the HTTP status alone is therefore not
 * enough — every case checks the database.
 *
 * <p>Secrets are fixed in {@code src/test/resources/application.properties}
 * (WEBHOOK_SECRET_GENERIC, WEBHOOK_SECRET_TYPEFORM, WEBHOOK_VERIFY_TOKEN_FACEBOOK).
 */
class LeadIngestIntegrationTest extends IntegrationTestBase {

    private static final String INGEST_URL = "/api/public/leads/ingest/";

    // Must match src/test/resources/application.properties
    private static final String GENERIC_SECRET  = "test-webhook-secret-generic";
    private static final String TYPEFORM_SECRET = "test-webhook-secret-typeform";
    private static final String FB_VERIFY_TOKEN = "test-facebook-verify-token";

    @Autowired
    private LeadRepository leadRepository;

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Flat JSON payload accepted by the "generic" mapper. Email must be unique per test. */
    private String genericPayload(String email) {
        return """
                {
                  "email": "%s",
                  "first_name": "Jean",
                  "last_name": "Dupont",
                  "phone": "+32470000000",
                  "company": "ACME",
                  "subject": "Demande de devis",
                  "message": "Bonjour, je souhaite un devis."
                }""".formatted(email);
    }

    /** Typeform-shaped payload (form_response.answers matched by field ref). */
    private String typeformPayload(String email) {
        return """
                {
                  "form_response": {
                    "answers": [
                      { "type": "email", "email": "%s", "field": { "ref": "email" } },
                      { "type": "short_text", "text": "Marie Curie", "field": { "ref": "full_name" } },
                      { "type": "phone_number", "phone_number": "+32471111111", "field": { "ref": "phone" } }
                    ]
                  }
                }""".formatted(email);
    }

    /** Computes the signature expected by HmacSha256Verifier: sha256=&lt;hex digest of body&gt;. */
    private String hmacSha256(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder("sha256=");
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /** Leads are persisted with a lowercased email — look up the same way. */
    private Optional<Lead> findLeadByEmail(String email) {
        String normalized = email.toLowerCase().trim();
        return leadRepository.findAll().stream()
                .filter(lead -> normalized.equals(lead.getEmail()))
                .findFirst();
    }

    // =========================================================================
    // Source "generic" — PLAIN_TOKEN (header X-Webhook-Token)
    // =========================================================================

    @Nested
    class SourceGeneric {

        @Test
        void tokenValide_creeLeLead() throws Exception {
            String email = "generic_ok@ingest-test.com";

            mockMvc.perform(post(INGEST_URL + "generic")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Webhook-Token", GENERIC_SECRET)
                            .content(genericPayload(email)))
                    .andExpect(status().isOk());

            Lead lead = findLeadByEmail(email).orElse(null);
            assertNotNull(lead, "Le lead doit être créé en base");
            assertEquals("Jean", lead.getFirstName());
            assertEquals("Dupont", lead.getLastName());
            assertEquals(LeadSource.OTHER, lead.getLeadSource());
        }

        @Test
        void tokenInvalide_retourne200SansCreerDeLead() throws Exception {
            String email = "generic_bad_token@ingest-test.com";

            // 200 volontaire : ne rien révéler à un attaquant
            mockMvc.perform(post(INGEST_URL + "generic")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Webhook-Token", "mauvais-token")
                            .content(genericPayload(email)))
                    .andExpect(status().isOk());

            assertTrue(findLeadByEmail(email).isEmpty(),
                    "Aucun lead ne doit être créé avec un token invalide");
        }

        @Test
        void tokenAbsent_retourne200SansCreerDeLead() throws Exception {
            String email = "generic_no_token@ingest-test.com";

            mockMvc.perform(post(INGEST_URL + "generic")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(genericPayload(email)))
                    .andExpect(status().isOk());

            assertTrue(findLeadByEmail(email).isEmpty(),
                    "Aucun lead ne doit être créé sans token");
        }
    }

    // =========================================================================
    // Source "typeform" — HMAC-SHA256 (header Typeform-Signature)
    // =========================================================================

    @Nested
    class SourceTypeform {

        @Test
        void signatureValide_creeLeLead() throws Exception {
            String email = "typeform_ok@ingest-test.com";
            String body = typeformPayload(email);

            mockMvc.perform(post(INGEST_URL + "typeform")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Typeform-Signature", hmacSha256(body, TYPEFORM_SECRET))
                            .content(body))
                    .andExpect(status().isOk());

            Lead lead = findLeadByEmail(email).orElse(null);
            assertNotNull(lead, "Le lead doit être créé en base");
            assertEquals("Marie", lead.getFirstName(), "full_name doit être splitté sur l'espace");
            assertEquals("Curie", lead.getLastName());
            assertEquals(LeadSource.TYPEFORM, lead.getLeadSource());
        }

        @Test
        void signatureInvalide_retourne200SansCreerDeLead() throws Exception {
            String email = "typeform_bad_sig@ingest-test.com";
            String body = typeformPayload(email);

            // Signature calculée avec un mauvais secret
            mockMvc.perform(post(INGEST_URL + "typeform")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Typeform-Signature", hmacSha256(body, "mauvais-secret"))
                            .content(body))
                    .andExpect(status().isOk());

            assertTrue(findLeadByEmail(email).isEmpty(),
                    "Aucun lead ne doit être créé avec une signature invalide");
        }

        @Test
        void signatureCalculeeSurUnAutreBody_retourne200SansCreerDeLead() throws Exception {
            // Rejeu : signature valide mais pour un autre payload
            String email = "typeform_replay@ingest-test.com";
            String body = typeformPayload(email);
            String otherBody = typeformPayload("autre@ingest-test.com");

            mockMvc.perform(post(INGEST_URL + "typeform")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Typeform-Signature", hmacSha256(otherBody, TYPEFORM_SECRET))
                            .content(body))
                    .andExpect(status().isOk());

            assertTrue(findLeadByEmail(email).isEmpty(),
                    "La signature doit couvrir le body exact de la requête");
        }

        @Test
        void signatureValideMaisJsonMalforme_retourne400() throws Exception {
            String body = "{ceci n'est pas du json";

            mockMvc.perform(post(INGEST_URL + "typeform")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Typeform-Signature", hmacSha256(body, TYPEFORM_SECRET))
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void signatureValideMaisPayloadSansEmail_retourne200SansCreerDeLead() throws Exception {
            String body = """
                    {
                      "form_response": {
                        "answers": [
                          { "type": "short_text", "text": "Sans Email", "field": { "ref": "full_name" } }
                        ]
                      }
                    }""";
            long leadCountBefore = leadRepository.count();

            mockMvc.perform(post(INGEST_URL + "typeform")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Typeform-Signature", hmacSha256(body, TYPEFORM_SECRET))
                            .content(body))
                    .andExpect(status().isOk());

            assertEquals(leadCountBefore, leadRepository.count(),
                    "Un payload sans email doit être ignoré sans créer de lead");
        }
    }

    // =========================================================================
    // Source inconnue
    // =========================================================================

    @Nested
    class SourceInconnue {

        @Test
        void post_retourne404() throws Exception {
            mockMvc.perform(post(INGEST_URL + "plateforme-inexistante")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"x@y.com\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void challenge_retourne404() throws Exception {
            mockMvc.perform(get(INGEST_URL + "plateforme-inexistante")
                            .param("hub.mode", "subscribe")
                            .param("hub.verify_token", "x")
                            .param("hub.challenge", "y"))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // Challenge Facebook (GET handshake)
    // =========================================================================

    @Nested
    class ChallengeFacebook {

        @Test
        void verifyTokenCorrect_retourneLeChallenge() throws Exception {
            mockMvc.perform(get(INGEST_URL + "facebook")
                            .param("hub.mode", "subscribe")
                            .param("hub.verify_token", FB_VERIFY_TOKEN)
                            .param("hub.challenge", "challenge-12345"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("challenge-12345"));
        }

        @Test
        void verifyTokenIncorrect_retourne403() throws Exception {
            mockMvc.perform(get(INGEST_URL + "facebook")
                            .param("hub.mode", "subscribe")
                            .param("hub.verify_token", "mauvais-token")
                            .param("hub.challenge", "challenge-12345"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void modeManquant_retourne400() throws Exception {
            mockMvc.perform(get(INGEST_URL + "facebook")
                            .param("hub.verify_token", FB_VERIFY_TOKEN))
                    .andExpect(status().isBadRequest());
        }
    }
}
