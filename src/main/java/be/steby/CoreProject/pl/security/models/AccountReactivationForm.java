package be.steby.CoreProject.pl.security.models;

import be.steby.CoreProject.bll.domains.account.models.DeactivationRequest;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.pl.security.validators.ValidDeactivationRequest;
import be.steby.CoreProject.pl.security.validators.ValidEmailDomain;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


@ValidDeactivationRequest
public record AccountReactivationForm(
    @NotBlank(message = "L'adresse email ne peut pas être vide")
    @Email(message = "Format d'email invalide")
    @Size(max = 254, message = "L'email ne peut pas dépasser 254 caractères")
    @ValidEmailDomain
    String email
) {}
