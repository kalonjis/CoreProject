package be.steby.CoreProject.pl.domains.password.validators;

import be.steby.CoreProject.bll.common.services.validation.password.PasswordPolicyService;
import be.steby.CoreProject.bll.domains.password.models.PasswordValidationResult;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

/**
 * Validator implementation for {@link StrongPassword} annotation.
 *
 * ✅ DRY: Delegates validation to PasswordPolicyService from password domain
 * This ensures consistent password rules across the entire application:
 * - PL layer validation (via this annotation)
 * - BLL layer validation (via PasswordPolicyService directly)
 * - Same configuration from application.properties
 */
@RequiredArgsConstructor
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    // ✅ INJECT the password policy service from password domain
    private final PasswordPolicyService passwordPolicyService;

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.trim().isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Password cannot be null or empty")
                    .addConstraintViolation();
            return false;
        }

        // ✅ DELEGATION: Use PasswordPolicyService for validation
        PasswordValidationResult result = passwordPolicyService.validatePassword(password);

        if (!result.isValid()) {
            // Disable default violation message
            context.disableDefaultConstraintViolation();

            // Add all specific error messages from the policy service
            result.errors().forEach(error ->
                    context.buildConstraintViolationWithTemplate(error)
                            .addConstraintViolation()
            );
        }

        return result.isValid();
    }
}