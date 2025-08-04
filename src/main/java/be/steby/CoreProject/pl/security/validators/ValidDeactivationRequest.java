package be.steby.CoreProject.pl.security.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Annotation de validation pour les demandes de désactivation de compte.
 * Valide que la demande respecte les règles métier définies dans DeactivationPolicyService.
 */
@Documented
@Constraint(validatedBy = DeactivationRequestValidator.class)
@Target({ElementType.TYPE})  // Au niveau de la classe/record
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDeactivationRequest {
    String message() default "La demande de désactivation ne respecte pas les critères requis";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}