package be.steby.CoreProject.bll.domains.emailAddress.models;

import java.util.List;

/**
 * Result class to hold email validation results.
 * Includes both validation status and detailed error messages.
 */
public record EmailValidationResult(boolean isValid, List<String> errors) {

    /**
     * Constructs a successful validation result with no errors.
     * @return A successful validation result
     */
    public static EmailValidationResult valid() {
        return new EmailValidationResult(true, List.of());
    }

    /**
     * Constructs a failed validation result with a single error message.
     * @param errorMessage The error message
     * @return A failed validation result
     */
    public static EmailValidationResult invalid(String errorMessage) {
        return new EmailValidationResult(false, List.of(errorMessage));
    }

    /**
     * Constructs a failed validation result with multiple error messages.
     * @param errorMessages The list of error messages
     * @return A failed validation result
     */
    public static EmailValidationResult invalid(List<String> errorMessages) {
        return new EmailValidationResult(false, errorMessages);
    }
}