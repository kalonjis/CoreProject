package be.steby.CoreProject.pl.domains.account.controller;

import be.steby.CoreProject.bll.domains.account.services.AccountService;
import be.steby.CoreProject.bll.domains.auth.services.cookies.AuthCookieService;
import be.steby.CoreProject.bll.domains.userRegistration.services.UserRegistrationService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.domains.account.models.requests.DeactivateAccountRequest;
import be.steby.CoreProject.pl.domains.account.models.requests.ReactivateAccountRequest;
import be.steby.CoreProject.pl.domains.account.models.requests.SignupRequest;
import be.steby.CoreProject.pl.domains.account.models.responses.AccountOperationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * REST Controller responsible for handling account lifecycle operations.
 * This controller manages account creation, activation, deactivation, and reactivation.
 * All business logic is delegated to appropriate services while this controller
 * focuses solely on HTTP concerns (status codes, headers, response formatting).
 */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final UserRegistrationService userRegistrationService;
    private final AccountService accountService;
    private final AuthCookieService authCookieService;

    // =========================================================================
    // PUBLIC ACCOUNT ENDPOINTS
    // =========================================================================

    /**
     * Handles account creation requests.
     * Creates a new user account and sends activation email.
     *
     * @param request Account signup request containing registration details
     * @param httpRequest HTTP request for context capture
     * @return ResponseEntity with 201 Created status and Location header
     */
    @PostMapping("/signup")
    @PreAuthorize("isAnonymous()")
    public ResponseEntity<AccountOperationResponse> signup(@Valid @RequestBody SignupRequest request,
                                       HttpServletRequest httpRequest) {
        log.info("Processing account signup request for username: {}", request.email());

        accountService.signup(request.toBllModel(), httpRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AccountOperationResponse.accountCreated());
    }

    /**
     * Handles account activation using token from email.
     * Activates the user account making it ready for login.
     *
     * @param token Activation token from email
     * @param request HTTP request for context capture
     * @return ResponseEntity with 200 OK status
     */
    @GetMapping("/activate")
    @PreAuthorize("isAnonymous()")
    public ResponseEntity<AccountOperationResponse> activate(@RequestParam String token,
                                                             HttpServletRequest request) {
        log.info("Processing account activation request with token");

        accountService.confirmNewUserAccount(token, request);

        log.info("Account activation successful");
        return ResponseEntity.ok(AccountOperationResponse.accountActivated());
    }

    /**
     * Handles requests for new activation token when original token expires.
     *
     * @param token Expired activation token
     * @param request HTTP request for context capture
     * @return ResponseEntity with 204 No Content status
     */
    @GetMapping("/resend-activation")
    @PreAuthorize("isAnonymous()")
    public ResponseEntity<AccountOperationResponse> requestActivation(@RequestParam String token,
                                                  HttpServletRequest request) {
        log.info("Processing request for new activation token");

        accountService.resendActivation(token, request);

        log.info("New activation token request processed");
        return ResponseEntity.ok(AccountOperationResponse.activationRequested());
    }

    // =========================================================================
    // AUTHENTICATED ACCOUNT ENDPOINTS
    // =========================================================================

    /**
     * Handles account deactivation requests from authenticated users.
     * Sends deactivation confirmation email to user.
     *
     * @param request Deactivation request containing reason and details
     * @param user Currently authenticated user
     * @param httpRequest HTTP request for context capture
     * @return ResponseEntity with 204 No Content status
     */
    @PostMapping("/request-deactivation")
    public ResponseEntity<AccountOperationResponse> requestDeactivation(@Valid @RequestBody DeactivateAccountRequest request,
                                                    @AuthenticationPrincipal User user,
                                                    HttpServletRequest httpRequest) {
        log.info("Processing account deactivation request for user: {}", user.getUsername());

        accountService.requestDeactivation(user, request.toBusiness(), httpRequest);

        log.info("Account deactivation request processed for user: {}", user.getUsername());
        return ResponseEntity.ok(AccountOperationResponse.deactivationRequested());
    }

    /**
     * Handles account deactivation confirmation using token from email.
     * Actually deactivates the user account.
     *
     * @param token Deactivation confirmation token from email
     * @param request HTTP request for context capture
     * @return ResponseEntity with 200 OK status
     */
    @GetMapping("/confirm-deactivation")
    public ResponseEntity<AccountOperationResponse> confirmDeactivation(@RequestParam String token,
                                                    HttpServletRequest request, HttpServletResponse response) {
        log.info("Processing account deactivation confirmation");

        accountService.deactivateAccount(token, request);

        authCookieService.clearAuthenticationCookies(response);

        log.info("Account deactivation confirmed and processed");
        return ResponseEntity.ok(AccountOperationResponse.accountDeactivated());
    }

    /**
     * Handles account reactivation requests from authenticated users.
     * Sends reactivation confirmation email to user.
     *
     * @param request Reactivation request containing reason and confirmation
     * @param httpRequest HTTP request for context capture
     * @return ResponseEntity with 204 No Content status
     */
    @PostMapping("/request-reactivation")
    public ResponseEntity<AccountOperationResponse> requestReactivation(@Valid @RequestBody ReactivateAccountRequest request,
                                                    HttpServletRequest httpRequest) {
        log.info("Processing account reactivation request for user: {}", request.identifier());

        accountService.requestReactivation(request.toBusiness(), httpRequest);

        log.info("Account reactivation request processed for user: {}", request.identifier());
        return ResponseEntity.ok(AccountOperationResponse.reactivationRequested());
    }

    /**
     * Handles account reactivation confirmation using token from email.
     * Actually reactivates the user account.
     *
     * @param token Reactivation confirmation token from email
     * @param request HTTP request for context capture
     * @return ResponseEntity with 200 OK status
     */
    @GetMapping("/confirm-reactivation")
    public ResponseEntity<AccountOperationResponse> confirmReactivation(@RequestParam String token,
                                                                        HttpServletRequest request) {
        log.info("Processing account reactivation confirmation");

        accountService.reactivateAccount(token, request);


        log.info("Account reactivation confirmed and processed");
        return ResponseEntity.ok(AccountOperationResponse.accountReactivated());
    }
}