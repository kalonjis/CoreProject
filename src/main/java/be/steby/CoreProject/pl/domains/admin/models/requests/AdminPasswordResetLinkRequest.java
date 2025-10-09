package be.steby.CoreProject.pl.domains.admin.models.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPasswordResetLinkRequest(
        @NotBlank(message = "Reason is required for audit purposes")
        @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
        String reason // could be replaced by IT ticket number - reference when implemented
) {
}