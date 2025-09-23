package be.steby.CoreProject.pl.domains.account.controller;

import be.steby.CoreProject.bll.domains.account.services.AccountService;
import be.steby.CoreProject.bll.domains.userRegistration.services.UserRegistrationService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.domains.account.models.requests.DeactivateAccountRequest;
import be.steby.CoreProject.pl.domains.account.models.requests.ReactivateAccountRequest;
import be.steby.CoreProject.pl.domains.account.models.requests.SignupRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request,
                                       HttpServletRequest httpRequest) {
        log.info("Processing account signup request for username: {}", request.username());

        User user = userRegistrationService.signup(request.toEntity(), httpRequest);
        String location = "/api/account/" + user.getId();

        log.info("Account signup successful for username: {}", user.getUsername());
        return ResponseEntity.created(URI.create(location)).build();
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
    public ResponseEntity<Void> activate(@RequestParam String token,
                                         HttpServletRequest request) {
        log.info("Processing account activation request with token");

        accountService.confirmNewUserAccount(token, request);

        log.info("Account activation successful");
        return ResponseEntity.ok().build();
    }

    /**
     * Handles requests for new activation token when original token expires.
     *
     * @param token Expired activation token
     * @param request HTTP request for context capture
     * @return ResponseEntity with 204 No Content status
     */
    @PostMapping("/request-activation")
    @PreAuthorize("isAnonymous()")
    public ResponseEntity<Void> requestActivation(@RequestParam String token,
                                                  HttpServletRequest request) {
        log.info("Processing request for new activation token");

        accountService.requestActivation(token, request);

        log.info("New activation token request processed");
        return ResponseEntity.noContent().build();
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
    public ResponseEntity<Void> requestDeactivation(@Valid @RequestBody DeactivateAccountRequest request,
                                                    @AuthenticationPrincipal User user,
                                                    HttpServletRequest httpRequest) {
        log.info("Processing account deactivation request for user: {}", user.getUsername());

        accountService.requestDeactivation(user, request.toBusiness(), httpRequest);

        log.info("Account deactivation request processed for user: {}", user.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Handles account deactivation confirmation using token from email.
     * Actually deactivates the user account.
     *
     * @param token Deactivation confirmation token from email
     * @param request HTTP request for context capture
     * @return ResponseEntity with 200 OK status
     */
    @PostMapping("/confirm-deactivation")
    public ResponseEntity<Void> confirmDeactivation(@RequestParam String token,
                                                    HttpServletRequest request) {
        log.info("Processing account deactivation confirmation");

        accountService.deactivateAccount(token, request);

        log.info("Account deactivation confirmed and processed");
        return ResponseEntity.ok().build();
    }

    /**
     * Handles account reactivation requests from authenticated users.
     * Sends reactivation confirmation email to user.
     *
     * @param request Reactivation request containing reason and confirmation
     * @param user Currently authenticated user
     * @param httpRequest HTTP request for context capture
     * @return ResponseEntity with 204 No Content status
     */
    @PostMapping("/request-reactivation")
    public ResponseEntity<Void> requestReactivation(@Valid @RequestBody ReactivateAccountRequest request,
                                                    @AuthenticationPrincipal User user,
                                                    HttpServletRequest httpRequest) {
        log.info("Processing account reactivation request for user: {}", user.getUsername());

        accountService.requestReactivation(user, httpRequest);

        log.info("Account reactivation request processed for user: {}", user.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Handles account reactivation confirmation using token from email.
     * Actually reactivates the user account.
     *
     * @param token Reactivation confirmation token from email
     * @param request HTTP request for context capture
     * @return ResponseEntity with 200 OK status
     */
    @PostMapping("/confirm-reactivation")
    public ResponseEntity<Void> confirmReactivation(@RequestParam String token,
                                                    HttpServletRequest request) {
        log.info("Processing account reactivation confirmation");

        accountService.reactivateAccount(token, request);

        log.info("Account reactivation confirmed and processed");
        return ResponseEntity.ok().build();
    }
}