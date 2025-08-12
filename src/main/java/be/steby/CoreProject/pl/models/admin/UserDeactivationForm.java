package be.steby.CoreProject.pl.models.admin;

import be.steby.CoreProject.dl.enums.AdminDeactivationCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserDeactivationForm(

        @NotNull
        AdminDeactivationCategory deactivationCategory,

        @NotBlank
        String adminDeactivationDetails
) {
}
