package be.steby.CoreProject.pl.security.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidEmailDomainValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmailDomain {

    String message() default "Domaine d'email non autorisé";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}