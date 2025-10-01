package be.steby.CoreProject.bll.domains.emailaddress.services;

import be.steby.CoreProject.bll.domains.emailaddress.models.EmailChangeRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for email address management operations.
 * Handles the complete email change flow with multi-step verification.
 */
public interface EmailAddressService {

    /**
     * Initiates an email change request.
     * Sends verification email to the current (old) email address.
     *
     * @param request Email change request containing new email and confirmation
     * @param httpRequest HTTP request for context capture
     */
    void changeEmailRequest(EmailChangeRequest request, HttpServletRequest httpRequest);

    /**
     * Cancels an email change process.
     * User clicks cancellation link from their old email address.
     *
     * @param token Cancellation token from email
     * @param httpRequest HTTP request for context capture
     */
    void cancelEmailChange(String token, HttpServletRequest httpRequest);

    /**
     * Verifies the email change initiation.
     * User confirms they initiated this change from their old email.
     * Sends verification link to the new email address.
     *
     * @param token Verification token from email
     * @param httpRequest HTTP request for context capture
     */
    void changeEmailVerification(String token, HttpServletRequest httpRequest);

    /**
     * Confirms and completes the email change.
     * User confirms from their new email address.
     * Sends confirmation emails to both old and new addresses.
     *
     * @param token Confirmation token from email
     * @param httpRequest HTTP request for context capture
     */
    void confirmEmail(String token, HttpServletRequest httpRequest);
}