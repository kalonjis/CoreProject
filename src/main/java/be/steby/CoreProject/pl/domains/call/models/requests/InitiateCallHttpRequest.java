package be.steby.CoreProject.pl.domains.call.models.requests;

import be.steby.CoreProject.bll.domains.crm.call.models.InitiateCallRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PL request model for initiating a call from the CRM.
 *
 * <p>At least one of {@code contactPublicId} or {@code leadPublicId} must be provided.
 * This constraint is enforced at the service layer to produce a domain-specific error.</p>
 *
 * @param phoneNumber     the number to dial (required)
 * @param contactPublicId public UUID of the contact being called; {@code null} if calling a lead
 * @param leadPublicId    public UUID of the lead being called; {@code null} if calling a contact
 */
public record InitiateCallHttpRequest(

        @NotBlank(message = "Phone number is required")
        @Size(max = 30, message = "Phone number must not exceed 30 characters")
        String phoneNumber,

        String contactPublicId,

        String leadPublicId

) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link InitiateCallRequest} for the service layer
     */
    public InitiateCallRequest toBllModel() {
        return new InitiateCallRequest(phoneNumber, contactPublicId, leadPublicId);
    }
}
