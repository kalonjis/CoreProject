package be.steby.CoreProject.pl.domains.admin.models.responses;

/**
 * Standard response for admin-initiated user account lifecycle operations.
 *
 * <p>Each static factory method maps to one operation, providing a consistent
 * message contract for the frontend.
 *
 * <p>Kept separate from {@code AccountOperationResponse} (self-service) to
 * maintain a clear distinction between user-initiated and admin-initiated actions.
 */
public record AdminAccountOperationResponse(String message) {

    // =========================================================================
    // Creation
    // =========================================================================

    public static AdminAccountOperationResponse userCreated() {
        return new AdminAccountOperationResponse(
                "User account created successfully. A temporary password has been sent to the user by email."
        );
    }

    // =========================================================================
    // Activation
    // =========================================================================

    public static AdminAccountOperationResponse userActivated() {
        return new AdminAccountOperationResponse(
                "User account activated successfully."
        );
    }

    // =========================================================================
    // Reactivation
    // =========================================================================

    public static AdminAccountOperationResponse userReactivated() {
        return new AdminAccountOperationResponse(
                "User account reactivated successfully."
        );
    }

    // =========================================================================
    // Deactivation
    // =========================================================================

    public static AdminAccountOperationResponse userDeactivated() {
        return new AdminAccountOperationResponse(
                "User account deactivated successfully."
        );
    }

    // =========================================================================
    // Deletion
    // =========================================================================

    public static AdminAccountOperationResponse userDeleted() {
        return new AdminAccountOperationResponse(
                "User account and all associated data have been permanently deleted."
        );
    }

    public static AdminAccountOperationResponse userGdprDeleted() {
        return new AdminAccountOperationResponse(
                "User personal data has been anonymized in compliance with GDPR right-to-erasure."
        );
    }
}