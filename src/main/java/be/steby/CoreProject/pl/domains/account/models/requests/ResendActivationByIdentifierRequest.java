package be.steby.CoreProject.pl.domains.account.models.requests;


import jakarta.validation.constraints.NotBlank;

public record ResendActivationByIdentifierRequest(
        @NotBlank String identifier
) {}

