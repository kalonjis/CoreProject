package be.steby.CoreProject.pl.models.user;

import be.steby.CoreProject.pl.security.validators.EmailsMatch;
import be.steby.CoreProject.pl.security.validators.ValidEmailDomain;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@EmailsMatch
public record ChangeEmailForm(

        @NotBlank(message = "L'adresse email ne peut pas être vide")
        @Email(message = "Format d'email invalide")
        @Size(max = 254, message = "L'email ne peut pas dépasser 254 caractères")
        @ValidEmailDomain
        String email,

        @NotBlank(message = "La confirmation ne peut pas être vide")
        @Email(message = "Format d'email invalide pour la confirmation")
        @Size(max = 254, message = "L'email de confirmation ne peut pas dépasser 254 caractères")
        String confirmEmail
) {
}