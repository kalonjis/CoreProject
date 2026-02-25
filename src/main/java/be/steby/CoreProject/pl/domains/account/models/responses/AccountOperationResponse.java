package be.steby.CoreProject.pl.domains.account.models.responses;

/**
 * Standard response record for account lifecycle operations.
 *
 * <p>Each static factory method maps to one account operation,
 * providing a consistent message contract for the frontend.
 */
public record AccountOperationResponse(
        String message,
        String username
) {

    // =========================================================================
    // Signup
    // =========================================================================

    public static AccountOperationResponse accountCreated() {
        return new AccountOperationResponse(
                "Account created successfully. Please check your email to activate your account.",
                null
        );
    }

    // =========================================================================
    // Activation
    // =========================================================================

    public static AccountOperationResponse accountActivated(String username) {
        return new AccountOperationResponse("Account activated successfully", username);
    }

    public static AccountOperationResponse activationResent() {
        return new AccountOperationResponse(
                "If your request was valid, a new activation link has been sent to your email.",
                null
        );
    }

    // =========================================================================
    // Deactivation (reversible)
    // =========================================================================

    public static AccountOperationResponse deactivationRequested() {
        return new AccountOperationResponse(
                "A confirmation email has been sent. Please check your inbox.",
                null
        );
    }

    public static AccountOperationResponse accountDeactivated() {
        return new AccountOperationResponse("Account deactivated successfully", null);
    }

    // =========================================================================
    // Reactivation
    // =========================================================================

    public static AccountOperationResponse reactivationRequested() {
        return new AccountOperationResponse(
                "A confirmation email has been sent. Please check your inbox.",
                null
        );
    }

    public static AccountOperationResponse accountReactivated(String username) {
        return new AccountOperationResponse("Account reactivated successfully", username);
    }

    // =========================================================================
    // GDPR Deletion (irreversible)
    // =========================================================================

    /**
     * Returned after the deletion request is submitted.
     * The user must still confirm via the email link — nothing is deleted yet.
     */
    public static AccountOperationResponse deletionRequested() {
        return new AccountOperationResponse(
                "A confirmation email has been sent. Please check your inbox to confirm your deletion request.",
                null
        );
    }

    /**
     * Returned after the user confirms via the email link and data has been anonymized.
     */
    public static AccountOperationResponse accountDeleted() {
        return new AccountOperationResponse(
                "Your account and personal data have been permanently deleted.",
                null
        );
    }
}