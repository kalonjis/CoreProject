package be.steby.CoreProject.pl.security.models;

import be.steby.CoreProject.pl.security.validators.PasswordMatch;
import be.steby.CoreProject.pl.security.validators.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


@PasswordMatch
public record ChangePasswordForm(
        @NotBlank(message = "Le mot de passe actuel ne peut pas être vide")
        @NotNull(message = "Le mot de passe actuel est obligatoire")
        String currentPassword,

        @NotBlank(message = "Le nouveau mot de passe ne peut pas être vide")
        @NotNull(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 8, max = 55, message = "Le nouveau mot de passe doit contenir entre 8 et 55 caractères")
        @StrongPassword
        String password,

        @NotBlank(message = "La confirmation du mot de passe ne peut pas être vide")
        @NotNull(message = "La confirmation du mot de passe est obligatoire")
        String confirmPassword
) {}