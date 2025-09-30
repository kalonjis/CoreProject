package be.steby.CoreProject.pl.domains.password.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation to ensure that two password fields match.
 *
 * <p>This annotation should be applied at the class level to compare two fields.
 * The default field names are "password" and "confirmPassword", but can be customized.
 *
 * <p>Usage examples:
 * <pre>
 * // Default field names (password and confirmPassword)
 * &#64;PasswordMatch
 * public class RegistrationRequest {
 *     private String password;
 *     private String confirmPassword;
 * }
 *
 * // Custom field names
 * &#64;PasswordMatch(first = "newPassword", second = "confirmNewPassword")
 * public class PasswordChangeRequest {
 *     private String newPassword;
 *     private String confirmNewPassword;
 * }
 * </pre>
 */
@Documented
@Constraint(validatedBy = PasswordMatchValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatch {

    String message() default "Password and confirm password do not match";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * The name of the first password field to compare.
     * Default is "password".
     */
    String first() default "password";

    /**
     * The name of the second password field to compare.
     * Default is "confirmPassword".
     */
    String second() default "confirmPassword";
}