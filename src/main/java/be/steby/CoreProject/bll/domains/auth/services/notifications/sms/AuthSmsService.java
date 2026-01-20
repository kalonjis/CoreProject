package be.steby.CoreProject.bll.domains.auth.services.notifications.sms;

import be.steby.CoreProject.bll.common.exceptions.phone.SmsSendingException;
import be.steby.CoreProject.dl.entities.User;

/**
 * Service interface for authentication-related SMS operations.
 * Handles 2FA verification SMS and security notifications.
 *
 * Provides both async and sync methods for different use cases:
 * - Async: For notifications where delivery confirmation is not critical
 * - Sync: For time-sensitive codes where immediate feedback is required
 */
public interface AuthSmsService {

    /**
     * Sends 2FA verification code via SMS (async).
     * Used during login flow - fire-and-forget.
     *
     * @param user User receiving the verification code
     * @param verificationCode 6-digit verification code
     */
    void sendTwoFactorVerificationCode(User user, String verificationCode);

    /**
     * Sends 2FA verification code via SMS synchronously (blocking).
     *
     * Use this for login 2FA where immediate feedback is required.
     * If delivery fails, throws exception so caller can offer alternatives.
     *
     * @param user User receiving the verification code
     * @param verificationCode 6-digit verification code
     * @throws SmsSendingException if SMS delivery fails
     */
    void sendTwoFactorCodeSync(User user, String verificationCode) throws SmsSendingException;
}