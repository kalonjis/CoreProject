package be.steby.CoreProject.pl.security.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        // Rien à initialiser
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // Désactiver le message par défaut
        context.disableDefaultConstraintViolation();

        // Vérifier si le mot de passe est null ou vide (autres annotations s'en occupent)
        if (password == null || password.isEmpty()) {
            return true;
        }

        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        boolean isValid = true;

        if (!hasUpperCase) {
            context.buildConstraintViolationWithTemplate("Le mot de passe doit contenir au moins une lettre majuscule")
                    .addConstraintViolation();
            isValid = false;
        }

        if (!hasLowerCase) {
            context.buildConstraintViolationWithTemplate("Le mot de passe doit contenir au moins une lettre minuscule")
                    .addConstraintViolation();
            isValid = false;
        }

        if (!hasDigit) {
            context.buildConstraintViolationWithTemplate("Le mot de passe doit contenir au moins un chiffre")
                    .addConstraintViolation();
            isValid = false;
        }

        if (!hasSpecialChar) {
            context.buildConstraintViolationWithTemplate("Le mot de passe doit contenir au moins un caractère spécial")
                    .addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }
}