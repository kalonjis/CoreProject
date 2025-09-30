package be.steby.CoreProject.pl.domains.password.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validator implementation for {@link StrongPassword} annotation.
 *
 * <p>Validates that a password meets strong security requirements including:
 * length, character diversity, and absence of common weak patterns.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    // Minimum password length
    private static final int MIN_LENGTH = 8;

    // Regex patterns for password requirements
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*");

    // Common weak patterns to reject
    private static final Pattern[] WEAK_PATTERNS = {
            Pattern.compile(".*123456.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*password.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*qwerty.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*admin.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*welcome.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*login.*", Pattern.CASE_INSENSITIVE)
    };

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.trim().isEmpty()) {
            addViolation(context, "Password cannot be null or empty");
            return false;
        }

        // Remove default violation message
        context.disableDefaultConstraintViolation();

        boolean isValid = true;

        // Check minimum length
        if (password.length() < MIN_LENGTH) {
            addViolation(context, "Password must be at least " + MIN_LENGTH + " characters long");
            isValid = false;
        }

        // Check for uppercase letter
        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            addViolation(context, "Password must contain at least one uppercase letter");
            isValid = false;
        }

        // Check for lowercase letter
        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            addViolation(context, "Password must contain at least one lowercase letter");
            isValid = false;
        }

        // Check for digit
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            addViolation(context, "Password must contain at least one digit");
            isValid = false;
        }

        // Check for special character
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            addViolation(context, "Password must contain at least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)");
            isValid = false;
        }

        // Check for weak patterns
        for (Pattern weakPattern : WEAK_PATTERNS) {
            if (weakPattern.matcher(password).matches()) {
                addViolation(context, "Password contains common weak patterns and is not secure");
                isValid = false;
                break; // Only report one weak pattern violation
            }
        }

        return isValid;
    }

    /**
     * Adds a custom violation message to the validation context.
     */
    private void addViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }
}
