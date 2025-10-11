package be.steby.CoreProject.pl.domains.admin.models.requests;

import be.steby.CoreProject.bll.domains.admin.models.password.AdminAlternativeChannelPasswordBLLRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request for sending temporary password via alternative channel.
 *
 * Uses PRE-REGISTERED alternative channels from the user's profile.
 * At least one channel (email or phone) must be selected.
 *
 * SECURITY: Only uses channels already verified and stored in the user profile.
 * Never accepts arbitrary email/phone addresses.
 */
public record AdminAlternativeChannelPasswordRequest(

        @NotBlank(message = "Reason is required for audit purposes")
        @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
        String reason,

        boolean useAlternativeEmail,

        boolean useAlternativePhone

) {
    /**
     * Custom validation: at least one channel must be selected.
     */
    public AdminAlternativeChannelPasswordRequest {
        if (!useAlternativeEmail && !useAlternativePhone) {
            throw new IllegalArgumentException(
                    "At least one alternative channel must be selected (email or phone)"
            );
        }
    }

    /**
     * Converts to BLL DTO.
     */
    public AdminAlternativeChannelPasswordBLLRequest toBLL() {
        return new AdminAlternativeChannelPasswordBLLRequest(
                reason != null ? reason.trim() : null,
                useAlternativeEmail,
                useAlternativePhone
        );
    }
}