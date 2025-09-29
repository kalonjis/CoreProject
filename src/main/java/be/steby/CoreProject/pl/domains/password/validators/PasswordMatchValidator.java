package be.steby.CoreProject.pl.domains.password.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Method;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, Object> {

    @Override
    public void initialize(PasswordMatch constraintAnnotation) {
        // Rien à initialiser
    }

    @Override
    public boolean isValid(Object form, ConstraintValidatorContext context) {
        String password = null;
        String confirmPassword = null;

        try {
            // Utilise la réflexion pour récupérer les propriétés
            Method passwordGetter = form.getClass().getMethod("password");
            Method confirmPasswordGetter = form.getClass().getMethod("confirmPassword");

            password = (String) passwordGetter.invoke(form);
            confirmPassword = (String) confirmPasswordGetter.invoke(form);
        } catch (Exception e) {
            // En cas d'erreur, retourne vrai pour éviter les faux positifs
            return true;
        }

        // Si l'un des champs est null, laissez les validation @NotNull s'en charger
        if (password == null || confirmPassword == null) {
            return true;
        }

        boolean isValid = password.equals(confirmPassword);

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Les mots de passe doivent être identiques")
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
        }

        return isValid;
    }
}