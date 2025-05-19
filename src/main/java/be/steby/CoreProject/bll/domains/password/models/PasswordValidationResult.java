package be.steby.CoreProject.bll.domains.password.models;

import java.util.List;

/**
 * Result class to hold password validation results.
 * Includes both validation status and detailed error messages.
 */
public record PasswordValidationResult(boolean isValid, List<String> errors) {
    /**
     * Constructs a successful validation result with no errors.
     * @return A successful validation result
     */
    public static PasswordValidationResult valid() {
        return new PasswordValidationResult(true, List.of());
    }

    /**
     * Constructs a failed validation result with a single error message.
     * @param errorMessage The error message
     * @return A failed validation result
     */
    public static PasswordValidationResult invalid(String errorMessage) {
        return new PasswordValidationResult(false, List.of(errorMessage));
    }
}