package be.steby.CoreProject.dl.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration of supported two-factor authentication methods.
 * 
 * This enum defines all available 2FA methods in the system,
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Getter
@RequiredArgsConstructor
public enum TwoFactorType {
    
    /**
     * Time-based One-Time Password (TOTP) using RFC 6238.
     * Compatible with authenticator apps like Google Authenticator, Authy, Microsoft Authenticator.
     * Most secure and recommended method, but requires a smartphone.
     */
    TOTP("Time-based OTP", "Authenticator app (smartphone required)", true),
    
    /**
     * SMS-based verification code.
     * Accessible for users with basic phones (no smartphone needed).
     * Less secure due to SIM swapping risks but widely accessible.
     * Good option for users without email access or authenticator apps.
     */
    SMS("SMS Code", "Phone number (basic phone works)", false),
    
    /**
     * Email-based verification code.
     * Useful as primary method for users without smartphones or hardware tokens.
     * Accessible from any device with email access (desktop, laptop, tablet).
     * Trade-off: Less secure if email account is compromised, but ensures accessibility.
     */
    EMAIL("Email Code", "Email address", false),
    
    /**
     * One-time backup codes for account recovery.
     * Generated once and stored securely. Each code can only be used once.
     * Essential for account recovery scenarios.
     */
    BACKUP_CODES("Backup Code", "Recovery codes", false),
    
    /**
     * WebAuthn/FIDO2 hardware security keys.
     * Most secure method using physical tokens (YubiKey, Titan Key, etc.).
     * Future implementation - not yet supported.
     */
    WEBAUTHN("Security Key", "Physical device", true);
    
    /**
     * Display name for UI purposes
     */
    private final String displayName;
    
    /**
     * Description of what's required for this method
     */
    private final String requirementDescription;
    
    /**
     * Whether this method is considered highly secure (phishing-resistant)
     */
    private final boolean highSecurity;
    
    /**
     * Check if this method requires user-provided input (phone/email)
     */
    public boolean requiresUserInput() {
        return this == SMS || this == EMAIL;
    }
    
    /**
     * Check if this method can be used as primary 2FA.
     * 
     * All methods except BACKUP_CODES can be primary to ensure flexibility.
     * While TOTP and WebAuthn are more secure, SMS and EMAIL provide better
     * accessibility for users without smartphones or hardware tokens.
     * 
     * Security consideration: Applications can choose to enforce TOTP/WebAuthn
     * only by checking isHighSecurity() separately if needed.
     */
    public boolean canBePrimary() {
        return this != BACKUP_CODES;
    }
    
    /**
     * Check if this method should only be used as backup
     */
    public boolean isBackupOnly() {
        return this == BACKUP_CODES;
    }
}