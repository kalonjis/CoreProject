package be.steby.CoreProject.pl.domains.account.models.responses;

/**
 * Response for account-related operations.
 */
public record AccountOperationResponse(
        String message
) {
    public static AccountOperationResponse accountCreated() {
        return new AccountOperationResponse(
                "Account created successfully. Please check your email to activate your account."
        );
    }

    public static AccountOperationResponse accountActivated() {
        return new AccountOperationResponse("Account activated successfully");
    }

    public static AccountOperationResponse activationRequested() {
        return new AccountOperationResponse(
                "If your request was valid, a new activation link has been sent to your email."
        );
    }

    public static AccountOperationResponse deactivationRequested() {
        return new AccountOperationResponse(
                "A confirmation email has been sent. Please check your inbox."
        );
    }

    public static AccountOperationResponse accountDeactivated() {
        return new AccountOperationResponse("Account deactivated successfully");
    }

    public static AccountOperationResponse reactivationRequested() {
        return new AccountOperationResponse(
                "A confirmation email has been sent. Please check your inbox."
        );
    }

    public static AccountOperationResponse accountReactivated() {
        return new AccountOperationResponse("Account reactivated successfully");
    }
}