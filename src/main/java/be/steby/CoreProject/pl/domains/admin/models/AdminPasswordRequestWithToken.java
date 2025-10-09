package be.steby.CoreProject.pl.domains.admin.models;

import jakarta.validation.constraints.NotBlank;

public record AdminPasswordRequestWithToken(
        @NotBlank
        String userPublicId
) {
}
