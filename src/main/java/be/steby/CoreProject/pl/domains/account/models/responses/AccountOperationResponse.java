package be.steby.CoreProject.pl.domains.account.models.responses;

/**
 * Response for account-related operations.
 */
public record AccountOperationResponse(
        String message,
        String username
) {
    public static AccountOperationResponse accountCreated() {
        return new AccountOperationResponse(
                "Account created successfully. Please check your email to activate your account.",
                null
        );
    }

    public static AccountOperationResponse accountActivated() {
        return new AccountOperationResponse("Account activated successfully",
                null
        );
    }

    public static AccountOperationResponse activationRequested() {
        return new AccountOperationResponse(
                "If your request was valid, a new activation link has been sent to your email.",
                null
        );
    }

    public static AccountOperationResponse deactivationRequested() {
        return new AccountOperationResponse(
                "A confirmation email has been sent. Please check your inbox.",
                null
        );
    }

    public static AccountOperationResponse accountDeactivated() {
        return new AccountOperationResponse("Account deactivated successfully",
                null
        );
    }

    public static AccountOperationResponse reactivationRequested() {
        return new AccountOperationResponse(
                "A confirmation email has been sent. Please check your inbox.",
                null
        );
    }

    public static AccountOperationResponse accountReactivated(String username) {
        return new AccountOperationResponse("Account reactivated successfully", username);
    }
}