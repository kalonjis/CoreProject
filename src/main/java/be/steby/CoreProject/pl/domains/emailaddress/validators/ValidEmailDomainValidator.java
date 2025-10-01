package be.steby.CoreProject.pl.domains.emailaddress.validators;

import be.steby.CoreProject.bll.domains.emailaddress.models.EmailValidationResult;
import be.steby.CoreProject.bll.common.services.validation.email.EmailPolicyService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ValidEmailDomainValidator implements ConstraintValidator<ValidEmailDomain, String> {

    private final EmailPolicyService emailPolicyService;

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isBlank()) {
            return true; // Let @NotBlank handle this
        }

        EmailValidationResult result = emailPolicyService.validateEmail(email);

        if (!result.isValid()) {
            // Disable default message and add custom ones
            context.disableDefaultConstraintViolation();
            result.errors().forEach(error ->
                    context.buildConstraintViolationWithTemplate(error)
                            .addConstraintViolation()
            );
        }

        return result.isValid();
    }
}