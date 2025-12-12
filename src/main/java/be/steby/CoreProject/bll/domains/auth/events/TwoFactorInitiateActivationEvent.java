package be.steby.CoreProject.bll.domains.auth.events;

import be.steby.CoreProject.dl.entities.User;

/**
 * Event published when a two-factor authentication activation process is initiated.
 * 
 * This event is triggered during the setup phase of 2FA when:
 * - A user starts the process to enable a new 2FA method
 * - A verification code needs to be sent to the user
 * - The activation token has been generated and stored in a cookie
 * 
 * The event contains the user information and the verification code that needs
 * to be sent via the appropriate channel (email, SMS, etc.).
 * 
 * This event is processed by TwoFactorNotificationListener which delegates
 * to the appropriate service (AuthMailerService, AuthSmsService) for actual
 * code delivery.
 * 
 * @param user the user initiating 2FA activation
 * @param verificationCode the 6-digit code to be sent for verification
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public record TwoFactorInitiateActivationEvent(
    User user,
    String verificationCode
) {
    
    /**
     * Validates the event parameters.
     * 
     * @throws IllegalArgumentException if any required parameter is null or invalid
     */
    public TwoFactorInitiateActivationEvent {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (verificationCode == null || verificationCode.isBlank()) {
            throw new IllegalArgumentException("Verification code cannot be null or blank");
        }
        if (!verificationCode.matches("\\d{6}")) {
            throw new IllegalArgumentException("Verification code must be exactly 6 digits");
        }
    }
}