package be.steby.CoreProject.bll.domains.auth.exceptions;

/**
 * Exception thrown when trying to verify TOTP setup but no setup is in progress.
 */
public class TwoFactorSetupNotFoundException extends AuthenticationException {

    public TwoFactorSetupNotFoundException(String message) {
        super(message, 404); // 404 Not Found
    }
    
    public static TwoFactorSetupNotFoundException forTOTP() {
        return new TwoFactorSetupNotFoundException(
            "No TOTP setup in progress for this user. Please start setup first."
        );
    }
}