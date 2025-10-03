package be.steby.CoreProject.bll.domains.account.models;

import java.util.List;

/**
 * Result of signup validation process.
 * Contains validation status and detailed error messages.
 */
public record SignupValidationResult(
        boolean isValid,
        List<String> errors
) {
    /**
     * Creates a valid result (no errors).
     */
    public static SignupValidationResult valid() {
        return new SignupValidationResult(true, List.of());
    }

    /**
     * Creates an invalid result with errors.
     */
    public static SignupValidationResult invalid(List<String> errors) {
        return new SignupValidationResult(false, errors);
    }
}