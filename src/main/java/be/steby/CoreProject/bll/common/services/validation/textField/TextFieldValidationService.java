package be.steby.CoreProject.bll.common.services.validation.textField;

import java.util.List;

/**
 * Validation service for basic text fields.
 * Centralizes common validations used across multiple domains.
 */
public interface TextFieldValidationService {

    /**
     * Validates a username according to standard rules.
     */
    void validateUsername(String username, List<String> errors);

    /**
     * Validates a first name according to standard rules.
     */
    void validateFirstname(String firstname, List<String> errors);

    /**
     * Validates a last name according to standard rules.
     */
    void validateLastname(String lastname, List<String> errors);

    /**
     * Validates a phone number according to standard rules.
     */
    void validatePhoneNumber(String phoneNumber, List<String> errors);

    /**
     * Validates a general text field with min/max length and optional pattern.
     */
    void validateText(String text, String fieldName, int minLength, int maxLength,
                      String pattern, List<String> errors);
}