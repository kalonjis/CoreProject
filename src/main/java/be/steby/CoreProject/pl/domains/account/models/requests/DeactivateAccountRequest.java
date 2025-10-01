package be.steby.CoreProject.pl.domains.account.models.requests;

import be.steby.CoreProject.bll.domains.account.models.DeactivationRequest;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request model for account deactivation operations.
 * Contains validation rules for user-initiated account deactivation.
 */
public record DeactivateAccountRequest(

        @NotNull(message = "Deactivation reason is required")
        DeactivationReason reason,

        @NotBlank(message = "Please provide details about your deactivation reason")
        @Size(min = 10, max = 500, message = "Reason details must be between 10 and 500 characters")
        String reasonDetails

) {
    /**
     * Converts this request to a DeactivationRequest for business logic processing.
     *
     * @return DeactivationRequest with data from this request
     */
    public DeactivationRequest toBusiness() {
        return new DeactivationRequest(reason, reasonDetails);
    }
}
