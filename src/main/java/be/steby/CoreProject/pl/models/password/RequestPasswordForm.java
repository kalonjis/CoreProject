package be.steby.CoreProject.pl.models.password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestPasswordForm (
        @Email
        @NotBlank @NotNull
        String email
) { }
