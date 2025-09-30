package be.steby.CoreProject.pl.domains.password.validators;

import be.steby.CoreProject.pl.domains.password.validators.StrongPasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation to ensure a password meets strong security requirements.
 *
 * <p>A strong password must:
 * <ul>
 *   <li>Be at least 8 characters long</li>
 *   <li>Contain at least one uppercase letter (A-Z)</li>
 *   <li>Contain at least one lowercase letter (a-z)</li>
 *   <li>Contain at least one digit (0-9)</li>
 *   <li>Contain at least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)</li>
 *   <li>Not contain common weak patterns</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 * public class PasswordRequest {
 *     &#64;StrongPassword
 *     private String password;
 * }
 * </pre>
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "Password must be at least 8 characters long and contain uppercase, lowercase, digit, and special character";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
