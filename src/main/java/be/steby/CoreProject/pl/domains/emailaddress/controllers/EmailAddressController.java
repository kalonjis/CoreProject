package be.steby.CoreProject.pl.domains.emailaddress.controllers;

import be.steby.CoreProject.bll.domains.emailaddress.services.EmailAddressService;
import be.steby.CoreProject.pl.domains.emailaddress.models.requests.ChangeEmailRequest;
import be.steby.CoreProject.pl.domains.emailaddress.models.responses.EmailAddressOperationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller responsible for handling email address management operations.
 * This controller manages email address changes through a secure multi-step verification process.
 * All business logic is delegated to EmailAddressService while this controller
 * focuses solely on HTTP concerns (status codes, headers, response formatting).
 *
 * Email Change Flow:
 * 1. POST /change-request - User initiates email change → verification email sent to OLD address
 * 2. GET /verify - User confirms initiation from OLD email → verification email sent to NEW address
 * 3. GET /confirm - User confirms from NEW email → confirmation emails sent to BOTH addresses
 * 4. GET /cancel - User can cancel the process at any time from OLD email
 */
@RestController
@RequestMapping("/api/email-address-change")
@RequiredArgsConstructor
@Slf4j
public class EmailAddressController {

    private final EmailAddressService emailAddressService;

    // =========================================================================
    // AUTHENTICATED EMAIL ADDRESS ENDPOINTS
    // =========================================================================

    /**
     * Handles email address change requests from authenticated users.
     * Initiates the email change process by sending a verification email
     * to the user's CURRENT (old) email address to confirm they initiated this change.
     *
     * Flow: User submits new email → Verification link sent to OLD address
     *
     * @param request Email change request containing the new email address
     * @param httpRequest HTTP request for context capture
     * @return ResponseEntity with success message
     */
    @PostMapping("/request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmailAddressOperationResponse> requestEmailChange(
            @Valid @RequestBody ChangeEmailRequest request,
            HttpServletRequest httpRequest) {

        log.info("Processing email change request");

        emailAddressService.changeEmailRequest(request.toBllModel(), httpRequest);

        log.info("Email change request processed successfully");
        return ResponseEntity.ok(EmailAddressOperationResponse.emailChangeRequested());
    }

    // =========================================================================
    // PUBLIC EMAIL ADDRESS ENDPOINTS (via email links)
    // =========================================================================

    /**
     * Handles email change cancellation using token from email.
     * Allows user to cancel the email change process from their OLD email address.
     * This is a GET endpoint because it's accessed via a clickable link in the email.
     *
     * Flow: User clicks cancellation link in OLD email → Process cancelled
     *
     * @param token Cancellation token from email
     * @param httpRequest HTTP request for context capture
     * @return ResponseEntity with success message
     */
    @GetMapping("/cancel")
    public ResponseEntity<EmailAddressOperationResponse> cancelEmailChange(
            @RequestParam String token,
            HttpServletRequest httpRequest) {

        log.info("Processing email change cancellation");

        emailAddressService.cancelEmailChange(token, httpRequest);

        log.info("Email change cancelled successfully");
        return ResponseEntity.ok(EmailAddressOperationResponse.emailChangeCancelled());
    }

    /**
     * Handles initiation verification using token from OLD email.
     * User confirms they initiated this email change request.
     * Once verified, a confirmation link is sent to the NEW email address.
     * This is a GET endpoint because it's accessed via a clickable link in the email.
     *
     * Flow: User clicks verification link in OLD email → Confirmation link sent to NEW address
     *
     * @param token Verification token from email
     * @param httpRequest HTTP request for context capture
     * @return ResponseEntity with success message
     */
    @GetMapping("/verify")
    public ResponseEntity<EmailAddressOperationResponse> verifyInitiation(
            @RequestParam String token,
            HttpServletRequest httpRequest) {

        log.info("Processing email change initiation verification");

        emailAddressService.changeEmailVerification(token, httpRequest);

        log.info("Email change initiation verified successfully");
        return ResponseEntity.ok(EmailAddressOperationResponse.emailVerified());
    }

    /**
     * Handles email change final confirmation using token from NEW email.
     * User confirms the email change from their NEW email address.
     * Once confirmed, confirmation emails are sent to BOTH addresses (old and new).
     * This is a GET endpoint because it's accessed via a clickable link in the email.
     *
     * Flow: User clicks confirmation link in NEW email → Change completed → Confirmation sent to BOTH addresses
     *
     * @param token Confirmation token from email
     * @param httpRequest HTTP request for context capture
     * @return ResponseEntity with success message
     */
    @GetMapping("/confirm")
    public ResponseEntity<EmailAddressOperationResponse> confirmEmailChange(
            @RequestParam String token,
            HttpServletRequest httpRequest) {

        log.info("Processing email change confirmation");

        emailAddressService.confirmEmail(token, httpRequest);

        log.info("Email change confirmed successfully");
        return ResponseEntity.ok(EmailAddressOperationResponse.emailChangeConfirmed());
    }
}