package be.steby.CoreProject.bll.domains.password.models;

/**
 * Business layer DTO for password reset with permission token.
 * 
 * <p>Contains the permission token (from successful SMS verification) and the new password.
 * Clean data transfer object for business logic processing.
 * 
 * <p>This model is used when a user has already verified their SMS code and
 * received a permission token that grants temporary access to reset their password.
 */
public record ResetPasswordWithPermissionBLLRequest(
        /**
         * The permission token received after successful SMS code verification.
         * Contains the user's email and grants temporary password reset access.
         */
        String permissionToken,
        
        /**
         * The new password to set for the user.
         * Already validated by presentation layer for strength requirements.
         */
        String newPassword
) {
    // Pure data transfer object - business validation in service layer
}