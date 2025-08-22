package be.steby.CoreProject.pl.models.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginForm(
        @NotBlank
        String username,

        @NotBlank
        String password
) {
}
