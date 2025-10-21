package be.steby.CoreProject.bll.domains.auth.services.phone;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for authentication-related SMS operations.
 * Handles 2FA verification SMS and security notifications.
 */
public interface AuthPhoneService {
    
    /**
     * Send two-factor authentication verification code via SMS
     * 
     * @param user User receiving the verification code
     * @param verificationCode 6-digit verification code
     * @param httpRequest HTTP request for context (IP, user agent, etc.)
     */
    void sendTwoFactorVerificationCode(User user, String verificationCode, HttpServletRequest httpRequest);
    

    /**
     * Send confirmation SMS when 2FA is enabled
     * 
     * @param user User who enabled 2FA
     * @param twoFactorType Type of 2FA that was enabled
     */
    void sendTwoFactorEnabledConfirmation(User user, TwoFactorType twoFactorType);
}