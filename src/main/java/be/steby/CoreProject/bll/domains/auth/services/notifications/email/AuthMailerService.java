package be.steby.CoreProject.bll.domains.auth.services.notifications.email;

import be.steby.CoreProject.bll.common.exceptions.mail.MailDeliveryException;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for authentication-related email operations.
 * Handles 2FA verification emails and security notifications.
 *
 * Provides both async and sync methods for different use cases:
 * - Async: For notifications where delivery confirmation is not critical
 * - Sync: For time-sensitive codes where immediate feedback is required
 */
public interface AuthMailerService {

    /**
     * Sends 2FA activation code via email (async).
     * Used during 2FA setup/configuration process.
     *
     * @param user User receiving the activation code
     * @param verificationCode 6-digit verification code
     */
    void sendTwoFactorActivationCode(User user, String verificationCode);

    /**
     * Sends 2FA verification code via email (async).
     * Used during login flow - fire-and-forget.
     *
     * @param user User receiving the verification code
     * @param verificationCode 6-digit verification code
     * @param httpRequest HTTP request for context (IP, user agent, etc.)
     */
    void sendTwoFactorVerificationCode(User user, String verificationCode, HttpServletRequest httpRequest);

    /**
     * Sends 2FA verification code via email synchronously (blocking).
     *
     * Use this for login 2FA where immediate feedback is required.
     * If delivery fails, throws exception so caller can offer alternatives.
     *
     * @param user User receiving the verification code
     * @param verificationCode 6-digit verification code
     * @param httpRequest HTTP request for context (IP, user agent, etc.)
     * @throws MailDeliveryException if email delivery fails
     */
    void sendTwoFactorCodeSync(User user, String verificationCode, HttpServletRequest httpRequest)
            throws MailDeliveryException;

    /**
     * Sends confirmation email when 2FA is enabled (async).
     *
     * @param user User who enabled 2FA
     * @param twoFactorType Type of 2FA that was enabled
     */
    void sendTwoFactorEnabledConfirmation(User user, TwoFactorType twoFactorType);
}