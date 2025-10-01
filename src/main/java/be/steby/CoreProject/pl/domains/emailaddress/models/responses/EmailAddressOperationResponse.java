package be.steby.CoreProject.pl.domains.emailaddress.models.responses;

/**
 * Response for email address-related operations.
 * Provides standardized messages for various email address management operations.
 */
public record EmailAddressOperationResponse(
        String message
) {
    /**
     * Response for successful email change request.
     * Indicates that verification emails have been sent to both old and new addresses.
     */
    public static EmailAddressOperationResponse emailChangeRequested() {
        return new EmailAddressOperationResponse(
                "Email change requested. Please check your inbox to verify the new email address."
        );
    }

    /**
     * Response for successful email address verification.
     */
    public static EmailAddressOperationResponse emailVerified() {
        return new EmailAddressOperationResponse(
                "Email address verified successfully."
        );
    }


    /**
     * Response for successful email change cancellation.
     */
    public static EmailAddressOperationResponse emailChangeCancelled() {
        return new EmailAddressOperationResponse(
                "Email change cancelled successfully. Your email address remains unchanged."
        );
    }

    /**
     * Response for successful email change confirmation.
     */
    public static EmailAddressOperationResponse emailChangeConfirmed() {
        return new EmailAddressOperationResponse(
                "Email change confirmed successfully. Your new email address is now active."
        );
    }
}