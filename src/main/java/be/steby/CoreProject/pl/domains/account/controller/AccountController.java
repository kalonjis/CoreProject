package be.steby.CoreProject.pl.domains.account.controller;

import be.steby.CoreProject.bll.domains.account.services.AccountService;
import be.steby.CoreProject.bll.domains.auth.services.cookies.AuthCookieService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.domains.account.models.requests.DeactivateAccountRequest;
import be.steby.CoreProject.pl.domains.account.models.requests.ReactivateAccountRequest;
import be.steby.CoreProject.pl.domains.account.models.requests.ResendActivationByIdentifierRequest;
import be.steby.CoreProject.pl.domains.account.models.requests.SignupRequest;
import be.steby.CoreProject.pl.domains.account.models.responses.AccountOperationResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for account lifecycle operations.
 *
 * <p>Handles signup, activation, deactivation, reactivation, and GDPR deletion.
 * All business logic is delegated to {@link AccountService}.
 * This controller is responsible for HTTP concerns only
 * (status codes, headers, response formatting).
 */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final AuthCookieService authCookieService;

    // =========================================================================
    // SIGNUP
    // =========================================================================

    /**
     * Creates a new user account and sends an activation email.
     *
     * <p>POST /api/account/signup
     *
     * @param request signup payload containing registration details
     * @return 201 Created
     */
    @PostMapping("/signup")
    @PreAuthorize("isAnonymous()")
    public ResponseEntity<AccountOperationResponse> signup(
            @Valid @RequestBody SignupRequest request) {
        log.info("Processing account signup request for email: {}", request.email());

        accountService.signup(request.toBllModel());

        log.info("Account signup processed for email: {}", request.email());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AccountOperationResponse.accountCreated());
    }

    // =========================================================================
    // ACTIVATION
    // =========================================================================

    /**
     * Activates a new user account using the token from the confirmation email.
     *
     * <p>GET /api/account/activate?token=
     *
     * @param token the raw token value from the activation email link
     * @return 200 OK
     */
    @GetMapping("/activate")
    public ResponseEntity<AccountOperationResponse> activate(
            @RequestParam String token) {
        log.info("Processing account activation");

        String username = accountService.confirmNewUserAccount(token).getUsername();

        log.info("Account activated for user: {}", username);
        return ResponseEntity.ok(AccountOperationResponse.accountActivated(username));
    }

    /**
     * Resends the activation email using the original token.
     *
     * <p>POST /api/account/resend-activation
     *
     * @param token the original activation token
     * @return 202 Accepted
     */
    @PostMapping("/resend-activation")
    public ResponseEntity<AccountOperationResponse> resendActivation(
            @RequestParam String token) {
        log.info("Processing activation resend");

        accountService.resendActivation(token);

        log.info("Activation email resent");
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(AccountOperationResponse.activationResent());
    }

    /**
     * Resends the activation email by email address or username.
     *
     * <p>POST /api/account/resend-activation-by-identifier
     *
     * @param request payload containing the email or username
     * @return 202 Accepted
     */
    @PostMapping("/resend-activation-by-identifier")
    public ResponseEntity<AccountOperationResponse> resendActivationByIdentifier(
            @Valid @RequestBody ResendActivationByIdentifierRequest request) {
        log.info("Processing activation resend by identifier");

        accountService.resendActivationByIdentifier(request.identifier());

        log.info("Activation email resent by identifier");
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(AccountOperationResponse.activationResent());
    }

    // =========================================================================
    // DEACTIVATION
    // =========================================================================

    /**
     * Initiates a self-deactivation request for the authenticated user.
     * Sends a confirmation email with a single-use link.
     *
     * <p>POST /api/account/request-deactivation
     *
     * @param user    the authenticated user
     * @param request deactivation payload containing reason and optional details
     * @return 202 Accepted
     */
    @PostMapping("/request-deactivation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountOperationResponse> requestDeactivation(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody DeactivateAccountRequest request) {
        log.info("Processing deactivation request for user: {}", user.getUsername());

        accountService.requestDeactivation(user, request.toBusiness());

        log.info("Deactivation request processed for user: {}", user.getUsername());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(AccountOperationResponse.deactivationRequested());
    }

    /**
     * Confirms and executes the deactivation via the token received by email.
     * Clears authentication cookies on success.
     *
     * <p>GET /api/account/confirm-deactivation?token=
     *
     * @param token    the raw token value from the confirmation email link
     * @param response the HTTP response — used to clear auth cookies
     * @return 200 OK
     */
    @GetMapping("/confirm-deactivation")
    public ResponseEntity<AccountOperationResponse> confirmDeactivation(
            @RequestParam String token,
            HttpServletResponse response) {
        log.info("Processing deactivation confirmation");

        String username = accountService.deactivateAccount(token).getUsername();
        authCookieService.clearAuthenticationCookies(response);

        log.info("Deactivation confirmed for user: {}", username);
        return ResponseEntity.ok(AccountOperationResponse.accountDeactivated());
    }

    // =========================================================================
    // REACTIVATION
    // =========================================================================

    /**
     * Initiates a reactivation request for a deactivated account.
     * Sends a confirmation email with a single-use link.
     *
     * <p>POST /api/account/request-reactivation
     *
     * @param request reactivation payload containing the account identifier
     * @return 202 Accepted
     */
    @PostMapping("/request-reactivation")
    public ResponseEntity<AccountOperationResponse> requestReactivation(
            @Valid @RequestBody ReactivateAccountRequest request) {
        log.info("Processing reactivation request");

        accountService.requestReactivation(request.toBusiness());

        log.info("Reactivation request processed");
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(AccountOperationResponse.reactivationRequested());
    }

    /**
     * Confirms and executes the reactivation via the token received by email.
     *
     * <p>GET /api/account/confirm-reactivation?token=
     *
     * @param token the raw token value from the confirmation email link
     * @return 200 OK
     */
    @GetMapping("/confirm-reactivation")
    public ResponseEntity<AccountOperationResponse> confirmReactivation(
            @RequestParam String token) {
        log.info("Processing reactivation confirmation");

        String username = accountService.reactivateAccount(token).getUsername();

        log.info("Reactivation confirmed for user: {}", username);
        return ResponseEntity.ok(AccountOperationResponse.accountReactivated(username));
    }

    // =========================================================================
    // GDPR DELETION
    // =========================================================================

    /**
     * Initiates a GDPR deletion request for the authenticated user.
     * Sends a confirmation email with a single-use link.
     * No data is modified at this stage.
     *
     * <p>POST /api/account/request-deletion
     *
     * @param user the authenticated user
     * @return 202 Accepted
     */
    @PostMapping("/request-deletion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountOperationResponse> requestDeletion(
            @AuthenticationPrincipal User user) {
        log.info("Processing GDPR deletion request for user: {}", user.getUsername());

        accountService.requestDeletion(user);

        log.info("GDPR deletion request processed for user: {}", user.getUsername());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(AccountOperationResponse.deletionRequested());
    }

    /**
     * Confirms and executes the GDPR deletion via the token received by email.
     * Anonymizes personal data, revokes all tokens, and clears authentication cookies.
     *
     * <p>GET /api/account/confirm-deletion?token=
     *
     * @param token    the raw token value from the confirmation email link
     * @param response the HTTP response — used to clear auth cookies
     * @return 200 OK
     */
    @GetMapping("/confirm-deletion")
    public ResponseEntity<AccountOperationResponse> confirmDeletion(
            @RequestParam String token,
            HttpServletResponse response) {
        log.info("Processing GDPR deletion confirmation");

        accountService.confirmDeletion(token);
        authCookieService.clearAuthenticationCookies(response);

        log.info("GDPR deletion confirmed and session cleared");
        return ResponseEntity.ok(AccountOperationResponse.accountDeleted());
    }
}