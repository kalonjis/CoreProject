package be.steby.CoreProject.pl.domains.emailaddress.validators;

import be.steby.CoreProject.pl.domains.emailaddress.models.requests.ChangeEmailRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for the @EmailsMatch annotation.
 * Ensures that email and confirmEmail fields match in ChangeEmailRequest.
 */
public class EmailsMatchValidator implements ConstraintValidator<EmailsMatch, ChangeEmailRequest> {

    @Override
    public boolean isValid(ChangeEmailRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // Let other validators handle null
        }

        if (request.email() == null || request.confirmEmail() == null) {
            return true; // Let @NotBlank handle nulls
        }

        boolean isValid = request.email().equals(request.confirmEmail());

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Email addresses must match")
                    .addPropertyNode("confirmEmail")
                    .addConstraintViolation();
        }

        return isValid;
    }
}