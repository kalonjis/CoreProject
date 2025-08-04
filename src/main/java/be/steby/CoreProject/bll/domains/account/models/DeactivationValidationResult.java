package be.steby.CoreProject.bll.domains.account.models;

import java.util.List;

/**
 * Result class to hold account deactivation validation results.
 * Includes both validation status and detailed error messages.
 */
public record DeactivationValidationResult(
        boolean isValid,
        List<String> errors
) {
    /**
     * Constructs a successful validation result with no errors.
     * @return A successful validation result
     */
    public static DeactivationValidationResult valid(){
        return new DeactivationValidationResult(true, List.of());
    }

    /**
     * Constructs a failed validation result with a single error message.
     * @param errorMessage The error message
     * @return A failed validation result
     */
    public static DeactivationValidationResult invalid(String errorMessage){
        return new DeactivationValidationResult(false, List.of(errorMessage));
    }

    /**
     * Constructs a failed validation result with multiple error messages.
     * @param errorMessages The error messages
     * @return A failed validation result
     */
    public static DeactivationValidationResult invalid(List<String> errorMessages){
        return new DeactivationValidationResult(false, errorMessages);
    }
}
