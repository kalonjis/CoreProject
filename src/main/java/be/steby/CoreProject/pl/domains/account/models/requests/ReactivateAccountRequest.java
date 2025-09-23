package be.steby.CoreProject.pl.domains.account.models.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request model for account reactivation operations.
 * Contains validation rules for user-initiated account reactivation.
 */
public record ReactivateAccountRequest(
        @JsonProperty("reactivation_reason")
        @NotBlank(message = "Please provide a reason for reactivating your account")
        @Size(min = 10, max = 300, message = "Reactivation reason must be between 10 and 300 characters")
        String reactivationReason,

        @JsonProperty("confirmation")
        @AssertTrue(message = "You must confirm that you want to reactivate your account")
        boolean confirmation,

        @JsonProperty("agree_to_terms")
        @AssertTrue(message = "You must agree to the current terms of service")
        boolean agreeToTerms
) {
    // Note: This request doesn't need a toBLL() method as the BLL service
    // for reactivation doesn't require additional parameters beyond the user
}