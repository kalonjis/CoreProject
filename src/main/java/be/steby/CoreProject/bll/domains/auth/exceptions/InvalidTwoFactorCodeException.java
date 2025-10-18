package be.steby.CoreProject.bll.domains.auth.exceptions;

/**
 * Exception thrown when a 2FA code (TOTP, SMS, etc.) is invalid.
 * Replaces generic IllegalArgumentException with domain-specific exception.
 */
public class InvalidTwoFactorCodeException extends AuthenticationException {

    public InvalidTwoFactorCodeException(String message) {
        super(message, 401); // 401 Unauthorized
    }

    public static InvalidTwoFactorCodeException forTOTP() {
        return new InvalidTwoFactorCodeException(
                "Invalid TOTP code. Please check your authenticator app and try again."
        );
    }


    public static InvalidTwoFactorCodeException forEmail() {
        return new InvalidTwoFactorCodeException(
                "Invalid email verification code. Please check your email and try again."
        );
    }


    public static InvalidTwoFactorCodeException forSMS() {
        return new InvalidTwoFactorCodeException(
                "Invalid SMS code. Please check the code and try again."
        );
    }
}