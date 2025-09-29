package be.steby.CoreProject.pl.domains.password.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ForgotPasswordRequest(
        @Email
        @NotBlank @NotNull
        String email
) { }
