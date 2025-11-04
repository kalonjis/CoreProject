package be.steby.CoreProject.bll.domains.password.models;

import be.steby.CoreProject.dl.enums.NotificationType;

/**
 * Business layer DTO for password reset request operations.
 * 
 * <p>This model represents a validated password reset request at the business logic level.
 * It contains clean, normalized data that has passed presentation layer validation
 * and is ready for business rule processing.
 *
 * <p>This separation allows:
 * <ul>
 *   <li>Business logic to work with clean, validated data</li>
 *   <li>Presentation layer to handle HTTP-specific concerns</li>
 *   <li>Easy testing of business logic without HTTP concerns</li>
 *   <li>Future API versioning without affecting business logic</li>
 * </ul>
 * 
 * <p><strong>Note:</strong> This is a pure data transfer object. Business validation
 * is performed in the service layer, not in this record.
 */
public record ForgotPasswordBLLRequest(
        /**
         * The user's email address (normalized to lowercase).
         * 
         * <p>This serves as the primary identifier to locate the user account.
         * The email has been validated by the presentation layer and normalized
         * for consistent processing.
         */
        String email,
        
        /**
         * The notification type for delivering the password reset.
         * 
         * <p>Determines how the reset code/link will be delivered:
         * <ul>
         *   <li>EMAIL: Traditional email with reset link</li>
         *   <li>SMS: Short verification code via SMS</li>
         * </ul>
         * 
         * <p>SMS delivery requires the user to have a verified phone number.
         * If requirements are not met, the system may silently take no action
         * for security reasons (preventing user enumeration).
         */
        NotificationType notificationType
) {}