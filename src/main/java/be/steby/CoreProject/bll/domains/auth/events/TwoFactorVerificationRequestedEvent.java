package be.steby.CoreProject.bll.domains.auth.events;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Event published when a 2FA verification code needs to be sent to the user.
 * Triggered during login initiation and code resend requests.
 */
public record TwoFactorVerificationRequestedEvent(
    User user,
    TwoFactorType twoFactorType,
    String verificationCode,    // Plain text code to send
    HttpServletRequest httpRequest,
    long timestamp
) {
    public TwoFactorVerificationRequestedEvent(User user, TwoFactorType twoFactorType, 
                                              String verificationCode, HttpServletRequest httpRequest) {
        this(user, twoFactorType, verificationCode, httpRequest, System.currentTimeMillis());
    }
}