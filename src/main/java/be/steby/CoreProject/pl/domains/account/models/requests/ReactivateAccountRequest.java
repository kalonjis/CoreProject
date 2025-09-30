package be.steby.CoreProject.pl.domains.account.models.requests;

import be.steby.CoreProject.bll.domains.account.models.ReactivationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request model for account reactivation operations.
 * Contains validation rules for user-initiated account reactivation.
 */
public record ReactivateAccountRequest(
        @NotBlank(message = "Email or username is required")
        @Size(max = 100, message = "Email or username cannot exceed 100 characters")
        String identifier  // email OR username
        )
{

    public ReactivationRequest toBusiness(){
        return new ReactivationRequest(normalizedIdentifier());
    }
    private String normalizedIdentifier() {
        return identifier != null ? identifier.toLowerCase().trim() : null;
    }
}