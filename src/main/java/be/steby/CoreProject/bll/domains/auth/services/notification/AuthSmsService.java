package be.steby.CoreProject.bll.domains.auth.services.notification;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for authentication-related SMS operations.
 * Handles 2FA verification SMS and security notifications.
 */
public interface AuthSmsService {
    
    /**
     * Send two-factor authentication verification code via SMS
     * 
     * @param user User receiving the verification code
     * @param verificationCode 6-digit verification code
     */
    void sendTwoFactorVerificationCode(User user, String verificationCode);

}