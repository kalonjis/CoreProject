package be.steby.CoreProject.pl.security.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordMatchValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatch {

    String message() default "Les mots de passe ne correspondent pas";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}