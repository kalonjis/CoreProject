package be.steby.CoreProject.integration.call;

import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dal.repositories.crm.CommercialSipConfigRepository;
import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CommercialSipConfig;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.utils.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the inbound caller identity lookup:
 * {@code GET /api/crm/calls/caller-info?number=}.
 *
 * <p>Covers the three resolution branches of {@code CallServiceImpl.resolveCallerInfo()}:
 * <ol>
 *   <li>SIP extension — matches {@code CommercialSipConfig.sipUsername}</li>
 *   <li>CRM contact — matches {@code Contact.phone} (exact)</li>
 *   <li>Unknown — falls back to the raw number as display name</li>
 * </ol>
 *
 * <p>Plus their priority (SIP wins over contact) and the access control of the
 * endpoint (COMMERCIAL/ADMIN required).
 */
class CallerInfoIntegrationTest extends IntegrationTestBase {

    private static final String CALLER_INFO_URL = "/api/crm/calls/caller-info";
    private static final String LOGIN_URL       = "/api/auth/login";
    private static final String TEST_PASSWORD   = "TestPass123!";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommercialSipConfigRepository sipConfigRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Access token of the logged-in commercial, shared by all lookup tests. */
    private Cookie commercialAccessToken;

    // =========================================================================
    // Setup
    // =========================================================================

    @BeforeEach
    void logInAsCommercial() throws Exception {
        String username = "callerinfo_commercial";
        if (userRepository.findByUsernameIgnoreCase(username).isEmpty()) {
            createActiveUser(username, UserRole.COMMERCIAL);
        }
        commercialAccessToken = login(username);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

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

    /** Logs in and returns the access_token cookie. */
    private Cookie login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + TEST_PASSWORD + "\"}")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("access_token");
        if (cookie != null) return cookie;

        // BaseCookieService writes cookies via Set-Cookie headers — parse manually if needed
        for (String header : result.getResponse().getHeaders("Set-Cookie")) {
            if (header.startsWith("access_token=")) {
                return new Cookie("access_token", header.split(";")[0].substring("access_token=".length()));
            }
        }
        throw new IllegalStateException("access_token cookie not found after login");
    }

    /**
     * Registers a SIP extension for a dedicated (non logged-in) commercial.
     * Identifiers must be unique per test (no rollback between methods).
     */
    private CommercialSipConfig createSipExtension(String key, String extension, String displayName) {
        User owner = createActiveUser("callerinfo_sip_" + key, UserRole.COMMERCIAL);
        owner.setFirstname("Alice");
        owner.setLastname("Merveille");
        userRepository.save(owner);

        return sipConfigRepository.save(CommercialSipConfig.builder()
                .user(owner)
                .sipUsername(extension)
                .encryptedSipPassword("encrypted-not-used")
                .displayName(displayName)
                .build());
    }

    private Contact createContact(String key, String phone) {
        return contactRepository.save(Contact.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .email("callerinfo_" + key + "@contact-test.com")
                .phone(phone)
                .build());
    }

    // =========================================================================
    // Contrôle d'accès
    // =========================================================================

    @Nested
    class ControleDacces {

        @Test
        void sansAuthentification_retourne401() throws Exception {
            mockMvc.perform(get(CALLER_INFO_URL).param("number", "+32470000000"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void roleUserSimple_retourne403() throws Exception {
            createActiveUser("callerinfo_user_simple", UserRole.USER);
            Cookie userToken = login("callerinfo_user_simple");

            mockMvc.perform(get(CALLER_INFO_URL)
                            .param("number", "+32470000000")
                            .cookie(userToken))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // Branche 1 — extension SIP
    // =========================================================================

    @Nested
    class ExtensionSip {

        @Test
        void extensionAvecDisplayName_retourneLeDisplayName() throws Exception {
            createSipExtension("display", "1001", "Support Interne");

            mockMvc.perform(get(CALLER_INFO_URL)
                            .param("number", "1001")
                            .cookie(commercialAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value("Support Interne"))
                    .andExpect(jsonPath("$.contactPublicId").isEmpty())
                    .andExpect(jsonPath("$.isInternalUser").value(true));
        }

        @Test
        void extensionSansDisplayName_fallbackPrenomNom() throws Exception {
            createSipExtension("fallback", "1002", null);

            mockMvc.perform(get(CALLER_INFO_URL)
                            .param("number", "1002")
                            .cookie(commercialAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value("Alice Merveille"))
                    .andExpect(jsonPath("$.isInternalUser").value(true));
        }
    }

    // =========================================================================
    // Branche 2 — contact CRM
    // =========================================================================

    @Nested
    class ContactCrm {

        @Test
        void telephoneConnu_retourneNomEtPublicId() throws Exception {
            Contact contact = createContact("connu", "+32476111222");

            mockMvc.perform(get(CALLER_INFO_URL)
                            .param("number", "+32476111222")
                            .cookie(commercialAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value("Jean Dupont"))
                    .andExpect(jsonPath("$.contactPublicId").value(contact.getPublicId()))
                    .andExpect(jsonPath("$.isInternalUser").value(false));
        }

        @Test
        void extensionSipPrioritaireSurContact_quandLesDeuxMatchent() throws Exception {
            // Même numéro enregistré comme extension SIP ET comme téléphone de contact
            createSipExtension("prio", "2001", "Extension Prioritaire");
            createContact("prio", "2001");

            mockMvc.perform(get(CALLER_INFO_URL)
                            .param("number", "2001")
                            .cookie(commercialAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value("Extension Prioritaire"))
                    .andExpect(jsonPath("$.isInternalUser").value(true));
        }
    }

    // =========================================================================
    // Branche 3 — inconnu
    // =========================================================================

    @Nested
    class NumeroInconnu {

        @Test
        void numeroInconnu_fallbackSurLeNumeroBrut() throws Exception {
            mockMvc.perform(get(CALLER_INFO_URL)
                            .param("number", "+32499999999")
                            .cookie(commercialAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value("+32499999999"))
                    .andExpect(jsonPath("$.contactPublicId").isEmpty())
                    .andExpect(jsonPath("$.isInternalUser").value(false));
        }

        @Test
        void numeroVide_retourneUnknown() throws Exception {
            mockMvc.perform(get(CALLER_INFO_URL)
                            .param("number", "")
                            .cookie(commercialAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").isEmpty())
                    .andExpect(jsonPath("$.contactPublicId").isEmpty())
                    .andExpect(jsonPath("$.isInternalUser").value(false));
        }
    }
}
