package be.steby.CoreProject.bll.domains.password.services;


import be.steby.CoreProject.bll.domains.password.models.PasswordValidationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PasswordPolicyServiceImpl implements PasswordPolicyService{

    @Value("${security.password.min-length:8}")
    private int minLength;

    @Value("${security.password.max-length:55}")
    private int maxLength;

    @Value("${security.password.require-uppercase:true}")
    private boolean requireUppercase;

    @Value("${security.password.require-lowercase:true}")
    private boolean requireLowercase;

    @Value("${security.password.require-digit:true}")
    private boolean requireDigit;

    @Value("${security.password.require-special-char:true}")
    private boolean requireSpecialChar;

    @Value("${security.password.special-chars:!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?}")
    private String specialChars;

    // Regular expression patterns for validation
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");

    /**
     * Validates a password against all configured policy rules.
     *
     * @param password The password to validate
     * @return PasswordValidationResult containing validation status and any error messages
     */
    @Override
    public PasswordValidationResult validatePassword(String password) {
        if (password == null) {
            return new PasswordValidationResult(false, List.of("Le mot de passe ne peut pas être null"));
        }

        List<String> validationErrors = new ArrayList<>();

        // Check length requirements
        if (password.length() < minLength) {
            validationErrors.add("Le mot de passe doit contenir au moins " + minLength + " caractères");
        }

        if (password.length() > maxLength) {
            validationErrors.add("Le mot de passe ne peut pas dépasser " + maxLength + " caractères");
        }

        // Check character requirements
        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).matches()) {
            validationErrors.add("Le mot de passe doit contenir au moins une lettre majuscule");
        }

        if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).matches()) {
            validationErrors.add("Le mot de passe doit contenir au moins une lettre minuscule");
        }

        if (requireDigit && !DIGIT_PATTERN.matcher(password).matches()) {
            validationErrors.add("Le mot de passe doit contenir au moins un chiffre");
        }

        if (requireSpecialChar && !containsSpecialChar(password)) {
            validationErrors.add("Le mot de passe doit contenir au moins un caractère spécial");
        }

        // Additional validation rules could be added here

        return new PasswordValidationResult(validationErrors.isEmpty(), validationErrors);
    }

    /**
     * Checks if a password contains at least one special character from the configured set.
     *
     * @param password The password to check
     * @return true if the password contains a special character, false otherwise
     */
    private boolean containsSpecialChar(String password) {
        // Create a pattern dynamically using the configured special characters
        Pattern specialCharPattern = Pattern.compile(".*[" + Pattern.quote(specialChars) + "].*");
        return specialCharPattern.matcher(password).matches();
    }

    /**
     * Generates a secure random password that meets all policy requirements.
     * Useful for generating temporary passwords.
     *
     * @return A random password that meets all policy requirements
     */
    @Override
    public String generateSecurePassword() {
        // Minimum length for generated password (at least minLength, but can be longer)
        int length = Math.max(minLength, 12); // Use at least 12 chars for generated passwords

        StringBuilder password = new StringBuilder();
        java.security.SecureRandom random = new java.security.SecureRandom();

        // Define character pools
        String upperCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";

        // Ensure at least one of each required character type
        if (requireUppercase) {
            password.append(upperCaseLetters.charAt(random.nextInt(upperCaseLetters.length())));
        }

        if (requireLowercase) {
            password.append(lowerCaseLetters.charAt(random.nextInt(lowerCaseLetters.length())));
        }

        if (requireDigit) {
            password.append(digits.charAt(random.nextInt(digits.length())));
        }

        if (requireSpecialChar && !specialChars.isEmpty()) {
            password.append(specialChars.charAt(random.nextInt(specialChars.length())));
        }

        // Fill the rest with random characters from all allowed types
        String allChars = "";
        if (requireUppercase) allChars += upperCaseLetters;
        if (requireLowercase) allChars += lowerCaseLetters;
        if (requireDigit) allChars += digits;
        if (requireSpecialChar) allChars += specialChars;

        while (password.length() < length) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the password to avoid predictable pattern
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            char temp = passwordArray[index];
            passwordArray[index] = passwordArray[i];
            passwordArray[i] = temp;
        }

        return new String(passwordArray);
    }

}
