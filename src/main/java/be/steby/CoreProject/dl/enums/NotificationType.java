package be.steby.CoreProject.dl.enums;

/**
 * Enumeration of notification delivery channels for password reset operations.
 * 
 * <p>This enum defines the available methods for delivering password reset codes
 * to users. Each type has different requirements and security considerations.
 */
public enum NotificationType {
    
    /**
     * Email notification delivery.
     * 
     * <p>Sends a password reset link via email. This is the traditional method
     * that works with long-lived tokens and URL-based reset flows.
     * 
     * <p>Requirements:
     * <ul>
     *   <li>User must have a valid email address (always satisfied)</li>
     *   <li>Email address must be accessible by the user</li>
     * </ul>
     */
    EMAIL,
    
    /**
     * SMS notification delivery.
     * 
     * <p>Sends a short verification code via SMS. This provides faster delivery
     * and works on basic mobile phones without internet access.
     * 
     * <p>Requirements:
     * <ul>
     *   <li>User must have a phone number configured</li>
     *   <li>Phone number must be verified</li>
     *   <li>SMS service must be available and configured</li>
     * </ul>
     * 
     * <p>If these requirements are not met, the system silently falls back
     * to email delivery for security reasons (no user enumeration).
     */
    SMS
}