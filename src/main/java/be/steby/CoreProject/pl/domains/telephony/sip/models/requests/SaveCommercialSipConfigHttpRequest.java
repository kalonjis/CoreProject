package be.steby.CoreProject.pl.domains.telephony.sip.models.requests;

import be.steby.CoreProject.bll.domains.crm.telephony.sip.models.SaveSipConfigRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PL request for creating or updating a commercial's SIP extension config.
 *
 * @param targetUserPublicId public UUID of the commercial to configure
 * @param sipUsername        SIP extension username on Asterisk (e.g. "1001" or "john.doe")
 * @param sipPassword        plain-text SIP password; encrypted before persistence
 * @param displayName        optional SIP From display name
 */
public record SaveCommercialSipConfigHttpRequest(

        @NotBlank(message = "Target user public ID is required")
        String targetUserPublicId,

        @NotBlank(message = "SIP username is required")
        @Size(max = 100, message = "SIP username must not exceed 100 characters")
        String sipUsername,

        @NotBlank(message = "SIP password is required")
        String sipPassword,

        @Size(max = 100, message = "Display name must not exceed 100 characters")
        String displayName

) {
    public SaveSipConfigRequest toBllModel() {
        return new SaveSipConfigRequest(targetUserPublicId, sipUsername, sipPassword, displayName);
    }
}
