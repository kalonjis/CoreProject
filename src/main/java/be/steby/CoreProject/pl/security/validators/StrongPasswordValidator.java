package be.steby.CoreProject.pl.security.validators;

import be.steby.CoreProject.bll.domain.password.models.PasswordValidationResult;
import be.steby.CoreProject.bll.domain.password.services.PasswordPolicyService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private final PasswordPolicyService policyService;


    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return true; // Let @NotNull/@NotBlank handle this
        }

        PasswordValidationResult result = policyService.validatePassword(password);

        if (!result.isValid()) {
            // Add validation messages
            context.disableDefaultConstraintViolation();
            result.errors().forEach(error ->
                    context.buildConstraintViolationWithTemplate(error)
                            .addConstraintViolation()
            );
        }

        return result.isValid();
    }
}