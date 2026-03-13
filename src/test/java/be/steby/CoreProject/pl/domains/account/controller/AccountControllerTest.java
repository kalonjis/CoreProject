package be.steby.CoreProject.pl.domains.account.controller;

import be.steby.CoreProject.bll.common.services.validation.email.EmailPolicyService;
import be.steby.CoreProject.bll.common.services.validation.password.PasswordPolicyService;
import be.steby.CoreProject.bll.domains.account.services.AccountService;
import be.steby.CoreProject.bll.domains.auth.services.cookies.AuthCookieService;
import be.steby.CoreProject.bll.domains.emailaddress.models.EmailValidationResult;
import be.steby.CoreProject.bll.domains.password.models.PasswordValidationResult;
import be.steby.CoreProject.bll.domains.password.services.cookies.PasswordCookieService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.dl.config.JpaAuditingConfig;
import be.steby.CoreProject.utils.WebMvcTestSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest slice tests for AccountController.
 *
 * <p>Tests HTTP concerns: routes, status codes, response body, delegation to services,
 * and @PreAuthorize access control.
 *
 * <p>Production SecurityConfig is excluded (too many infrastructure dependencies for
 * the web slice) and replaced by the minimal WebMvcTestSecurityConfig. @PreAuthorize
 * annotations remain active via @EnableMethodSecurity.
 */
@WebMvcTest(
        value = AccountController.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "be\\.steby\\.CoreProject\\.il\\..*"
                ),
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = JpaAuditingConfig.class
                )
        }
)
@Import(WebMvcTestSecurityConfig.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private AuthCookieService authCookieService;

    @MockitoBean
    private EmailPolicyService emailPolicyService;

    @MockitoBean
    private PasswordPolicyService passwordPolicyService;

    @MockitoBean
    private PasswordCookieService passwordCookieService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        // Custom validators pass by default
        lenient().when(emailPolicyService.validateEmail(anyString())).thenReturn(EmailValidationResult.valid());
        lenient().when(passwordPolicyService.validatePassword(anyString())).thenReturn(PasswordValidationResult.valid());

        mockUser = new User();
        mockUser.setUsername("alice");
        mockUser.getUserRoles().add(UserRole.USER);
    }

    /** Creates an Authentication containing our User entity as principal. */
    private RequestPostProcessor authenticatedAs(User user) {
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities()
        );
        return authentication(auth);
    }

    // =========================================================================
    // POST /api/account/signup
    // =========================================================================

    @Nested
    class Signup {

        private Map<String, String> validPayload() {
            return Map.of(
                    "username", "alice_42",
                    "email", "alice@example.com",
                    "password", "Str0ng!Pass",
                    "confirmPassword", "Str0ng!Pass"
            );
        }

        @Test
        void requêteValide_retourne201() throws Exception {
            when(accountService.signup(any())).thenReturn(mockUser);

            mockMvc.perform(post("/api/account/signup")
                            .with(anonymous())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPayload())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").exists());

            verify(accountService).signup(any());
        }

        @Test
        void payloadInvalide_retourne400() throws Exception {
            var invalidPayload = Map.of(
                    "username", "ab",        // trop court (min=3)
                    "email", "alice@example.com",
                    "password", "Str0ng!Pass",
                    "confirmPassword", "Str0ng!Pass"
            );

            mockMvc.perform(post("/api/account/signup")
                            .with(anonymous())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidPayload)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void utilisateurAuthentifié_retourne403() throws Exception {
            mockMvc.perform(post("/api/account/signup")
                            .with(authenticatedAs(mockUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPayload())))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // GET /api/account/activate
    // =========================================================================

    @Nested
    class Activate {

        @Test
        void tokenValide_retourne200AvecUsername() throws Exception {
            when(accountService.confirmNewUserAccount("abc123")).thenReturn(mockUser);

            mockMvc.perform(get("/api/account/activate")
                            .param("token", "abc123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("alice"));
        }
    }

    // =========================================================================
    // POST /api/account/resend-activation
    // =========================================================================

    @Nested
    class ResendActivation {

        @Test
        void tokenValide_retourne202() throws Exception {
            mockMvc.perform(post("/api/account/resend-activation")
                            .param("token", "abc123"))
                    .andExpect(status().isAccepted());

            verify(accountService).resendActivation("abc123");
        }

        @Test
        void parIdentifiant_requêteValide_retourne202() throws Exception {
            mockMvc.perform(post("/api/account/resend-activation-by-identifier")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"identifier\":\"alice_42\"}"))
                    .andExpect(status().isAccepted());

            verify(accountService).resendActivationByIdentifier("alice_42");
        }
    }

    // =========================================================================
    // POST /api/account/request-deactivation
    // =========================================================================

    @Nested
    class RequestDeactivation {

        private String validPayload() throws Exception {
            return objectMapper.writeValueAsString(Map.of(
                    "reason", "TAKING_A_BREAK",
                    "reasonDetails", "Je prends une pause bien méritée."
            ));
        }

        @Test
        void utilisateurAnonyme_retourne403() throws Exception {
            mockMvc.perform(post("/api/account/request-deactivation")
                            .with(anonymous())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPayload()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void utilisateurAuthentifié_retourne202() throws Exception {
            mockMvc.perform(post("/api/account/request-deactivation")
                            .with(authenticatedAs(mockUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPayload()))
                    .andExpect(status().isAccepted());

            verify(accountService).requestDeactivation(any(), any());
        }
    }

    // =========================================================================
    // GET /api/account/confirm-deactivation
    // =========================================================================

    @Nested
    class ConfirmDeactivation {

        @Test
        void tokenValide_retourne200EtEffaceLesCookies() throws Exception {
            when(accountService.deactivateAccount("abc123")).thenReturn(mockUser);

            mockMvc.perform(get("/api/account/confirm-deactivation")
                            .param("token", "abc123"))
                    .andExpect(status().isOk());

            verify(authCookieService).clearAuthenticationCookies(any());
        }
    }

    // =========================================================================
    // POST /api/account/request-reactivation
    // =========================================================================

    @Nested
    class RequestReactivation {

        @Test
        void requêteValide_retourne202() throws Exception {
            mockMvc.perform(post("/api/account/request-reactivation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"identifier\":\"alice_42\"}"))
                    .andExpect(status().isAccepted());

            verify(accountService).requestReactivation(any());
        }
    }

    // =========================================================================
    // GET /api/account/confirm-reactivation
    // =========================================================================

    @Nested
    class ConfirmReactivation {

        @Test
        void tokenValide_retourne200AvecUsername() throws Exception {
            when(accountService.reactivateAccount("abc123")).thenReturn(mockUser);

            mockMvc.perform(get("/api/account/confirm-reactivation")
                            .param("token", "abc123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("alice"));
        }
    }

    // =========================================================================
    // POST /api/account/request-deletion
    // =========================================================================

    @Nested
    class RequestDeletion {

        @Test
        void utilisateurAnonyme_retourne403() throws Exception {
            mockMvc.perform(post("/api/account/request-deletion")
                            .with(anonymous()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void utilisateurAuthentifié_retourne202() throws Exception {
            mockMvc.perform(post("/api/account/request-deletion")
                            .with(authenticatedAs(mockUser)))
                    .andExpect(status().isAccepted());

            verify(accountService).requestDeletion(any());
        }
    }

    // =========================================================================
    // GET /api/account/confirm-deletion
    // =========================================================================

    @Nested
    class ConfirmDeletion {

        @Test
        void tokenValide_retourne200EtEffaceLesCookies() throws Exception {
            mockMvc.perform(get("/api/account/confirm-deletion")
                            .param("token", "abc123"))
                    .andExpect(status().isOk());

            verify(authCookieService).clearAuthenticationCookies(any());
        }
    }
}
