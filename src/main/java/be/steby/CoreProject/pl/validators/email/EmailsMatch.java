package be.steby.CoreProject.pl.validators.email;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EmailsMatchValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EmailsMatch {

    String message() default "Les adresses email doivent être identiques";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}