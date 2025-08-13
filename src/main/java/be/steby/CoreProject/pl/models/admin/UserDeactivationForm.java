package be.steby.CoreProject.pl.models.admin;

import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulaire de désactivation utilisateur par admin - Version simplifiée
 * Annotations natives seulement - Simple et efficace
 */
public record UserDeactivationForm(

        @NotNull(message = "La catégorie de désactivation est obligatoire")
        AdminDeactivationCategory deactivationCategory,

        @NotBlank(message = "Les détails de désactivation sont obligatoires")
        @Size(min = 10, max = 500, message = "Les détails doivent contenir entre 10 et 500 caractères")
        String adminDeactivationDetails

) {}