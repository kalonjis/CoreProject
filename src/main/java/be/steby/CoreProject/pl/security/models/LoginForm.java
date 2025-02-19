package be.steby.CoreProject.pl.security.models;

import jakarta.validation.constraints.NotBlank;

public record LoginForm(
        @NotBlank
        String username,

        @NotBlank
        String password
) {
}
