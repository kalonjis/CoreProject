package be.steby.CoreProject.pl.models.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record AccountReactivationForm(
    @NotBlank(message = "L'adresse email ne peut pas être vide")
    @Email(message = "Format d'email invalide")
    @Size(max = 254, message = "L'email ne peut pas dépasser 254 caractères")
    String email
) {}
