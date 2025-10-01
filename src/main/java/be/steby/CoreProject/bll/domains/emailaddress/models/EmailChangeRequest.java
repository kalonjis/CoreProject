package be.steby.CoreProject.bll.domains.emailaddress.models;

import be.steby.CoreProject.bll.domains.emailaddress.exceptions.EmailValidationException;

/**
 * Business layer model representing an email change request.
 * This DTO is used by the service layer and contains business validation logic.
 * It does not contain presentation-specific validation annotations.
 */
public record EmailChangeRequest(
        /**
         * New email address requested by the user.
         */
        String newEmail,

        /**
         * Confirmation of the new email address.
         * Must match newEmail exactly.
         */
        String confirmEmail
) {
    /**
     * Compact canonical constructor with business validation.
     * Ensures data integrity at the business layer level (defense in depth).
     *
     * @throws EmailValidationException if emails don't match or are invalid
     */
    public EmailChangeRequest {
        if (newEmail == null || newEmail.isBlank()) {
            throw new EmailValidationException("New email address cannot be empty");
        }
        if (confirmEmail == null || confirmEmail.isBlank()) {
            throw new EmailValidationException("Email confirmation cannot be empty");
        }
        if (!newEmail.equals(confirmEmail)) {
            throw new EmailValidationException("Email addresses must match");
        }
    }
}