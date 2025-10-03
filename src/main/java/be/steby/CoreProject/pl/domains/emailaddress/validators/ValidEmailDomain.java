package be.steby.CoreProject.pl.domains.emailaddress.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidEmailDomainValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmailDomain {

    String message() default "Email domain not allowed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}