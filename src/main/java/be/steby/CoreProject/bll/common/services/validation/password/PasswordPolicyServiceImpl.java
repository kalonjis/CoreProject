package be.steby.CoreProject.bll.common.services.validation.password;

import be.steby.CoreProject.bll.domains.password.models.PasswordValidationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

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
            return new PasswordValidationResult(false, List.of("Password cannot be null"));
        }

        List<String> validationErrors = new ArrayList<>();

        // Check length requirements
        if (password.length() < minLength) {
            validationErrors.add("Password must be at least " + minLength + " characters long");
        }

        if (password.length() > maxLength) {
            validationErrors.add("Password cannot exceed " + maxLength + " characters");
        }

        // Check character requirements
        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).matches()) {
            validationErrors.add("Password must contain at least one uppercase letter");
        }

        if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).matches()) {
            validationErrors.add("Password must contain at least one lowercase letter");
        }

        if (requireDigit && !DIGIT_PATTERN.matcher(password).matches()) {
            validationErrors.add("Password must contain at least one digit");
        }

        if (requireSpecialChar && !containsSpecialChar(password)) {
            validationErrors.add("Password must contain at least one special character");
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
     * @return A randomly generated password that satisfies all policy requirements
     */
    @Override
    public String generateSecurePassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        // Character pools
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = specialChars;

        // Ensure at least one character from each required category
        if (requireUppercase) {
            password.append(uppercase.charAt(random.nextInt(uppercase.length())));
        }
        if (requireLowercase) {
            password.append(lowercase.charAt(random.nextInt(lowercase.length())));
        }
        if (requireDigit) {
            password.append(digits.charAt(random.nextInt(digits.length())));
        }
        if (requireSpecialChar) {
            password.append(special.charAt(random.nextInt(special.length())));
        }

        // Fill the rest with random characters from all pools
        String allChars = uppercase + lowercase + digits + special;
        int remainingLength = minLength - password.length();

        for (int i = 0; i < remainingLength; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the password to avoid predictable patterns
        return shuffleString(password.toString(), random);
    }

    /**
     * Shuffles the characters in a string randomly.
     *
     * @param input The string to shuffle
     * @param random The SecureRandom instance to use
     * @return The shuffled string
     */
    private String shuffleString(String input, SecureRandom random) {
        char[] characters = input.toCharArray();
        for (int i = characters.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = characters[i];
            characters[i] = characters[j];
            characters[j] = temp;
        }
        return new String(characters);
    }
}