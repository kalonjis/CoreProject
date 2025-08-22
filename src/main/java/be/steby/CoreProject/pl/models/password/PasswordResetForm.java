package be.steby.CoreProject.pl.models.password;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import be.steby.CoreProject.pl.security.validators.PasswordMatch;
import be.steby.CoreProject.pl.security.validators.StrongPassword;

@PasswordMatch(message = "Les mots de passe doivent être identiques")
public record PasswordResetForm(
        @NotBlank(message = "Le mot de passe ne peut pas être vide")
        @NotNull(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 55, message = "Le mot de passe doit contenir entre 8 et 55 caractères")
        @StrongPassword
        String password,

        @NotBlank(message = "La confirmation du mot de passe ne peut pas être vide")
        @NotNull(message = "La confirmation du mot de passe est obligatoire")
        String confirmPassword
) {}