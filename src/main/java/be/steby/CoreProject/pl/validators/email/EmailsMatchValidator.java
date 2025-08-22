package be.steby.CoreProject.pl.validators.email;

import be.steby.CoreProject.pl.models.emailAddress.ChangeEmailForm;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EmailsMatchValidator implements ConstraintValidator<EmailsMatch, ChangeEmailForm> {

    @Override
    public boolean isValid(ChangeEmailForm form, ConstraintValidatorContext context) {
        if (form.email() == null || form.confirmEmail() == null) {
            return true; // Let @NotBlank handle nulls
        }

        boolean isValid = form.email().equals(form.confirmEmail());

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Les adresses email doivent être identiques")
                    .addPropertyNode("confirmEmail")
                    .addConstraintViolation();
        }

        return isValid;
    }
}