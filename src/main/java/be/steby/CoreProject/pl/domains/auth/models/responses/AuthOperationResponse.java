package be.steby.CoreProject.pl.domains.auth.models.responses;

/**
 * Response for authentication-related operations.
 * Follows the same pattern as PasswordOperationResponse.
 */
public record AuthOperationResponse(
        String message
) {
    public static AuthOperationResponse loginSuccessful() {
        return new AuthOperationResponse("Login successful");
    }

    public static AuthOperationResponse logoutSuccessful() {
        return new AuthOperationResponse("Logout successful");
    }

    public static AuthOperationResponse tokenRefreshed() {
        return new AuthOperationResponse("Token refreshed successfully");
    }

    public static AuthOperationResponse twoFactorRequired() {
        return new AuthOperationResponse("Two-factor authentication required");
    }

    public static AuthOperationResponse twoFactorCodeResent() {
        return new AuthOperationResponse("Verification code sent successfully");
    }
}