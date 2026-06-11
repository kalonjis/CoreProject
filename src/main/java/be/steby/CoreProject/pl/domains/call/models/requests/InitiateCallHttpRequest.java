package be.steby.CoreProject.pl.domains.call.models.requests;

import be.steby.CoreProject.bll.domains.crm.call.models.InitiateCallRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PL request model for initiating a call from the CRM.
 *
 * <p>For OUTBOUND calls, at least one of {@code contactPublicId} or {@code leadPublicId}
 * must be provided. This constraint is enforced at the service layer.</p>
 * <p>For INBOUND calls ({@code direction = "INBOUND"}), contact/lead may be unknown
 * at answer time — the service layer skips the constraint.</p>
 *
 * @param phoneNumber     the number to dial / caller number (required)
 * @param contactPublicId public UUID of the contact being called; {@code null} if lead or inbound
 * @param leadPublicId    public UUID of the lead being called; {@code null} if contact or inbound
 * @param direction       {@code "INBOUND"} or {@code "OUTBOUND"} (default when null)
 */
public record InitiateCallHttpRequest(

        @NotBlank(message = "Phone number is required")
        @Size(max = 30, message = "Phone number must not exceed 30 characters")
        String phoneNumber,

        String contactPublicId,

        String leadPublicId,

        String direction

) {

    public InitiateCallRequest toBllModel() {
        return new InitiateCallRequest(phoneNumber, contactPublicId, leadPublicId, direction);
    }
}
