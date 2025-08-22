package be.steby.CoreProject.pl.validators.password;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    String message() default "Le mot de passe ne respecte pas les critères de sécurité";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}


