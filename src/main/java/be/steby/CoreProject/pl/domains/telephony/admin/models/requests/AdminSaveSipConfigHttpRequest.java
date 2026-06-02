package be.steby.CoreProject.pl.domains.telephony.admin.models.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin request for assigning or updating a SIP extension config on a user.
 *
 * <p>The {@code targetUserPublicId} is taken from the URL path — not the body.
 * Password is always required (create and update alike).</p>
 *
 * @param sipUsername SIP extension username on Asterisk (e.g. "1001" or "john.doe")
 * @param sipPassword plain-text SIP password; encrypted before persistence
 * @param displayName optional SIP From display name; null uses the user's full name
 */
public record AdminSaveSipConfigHttpRequest(

        @NotBlank(message = "SIP username is required")
        @Size(max = 100, message = "SIP username must not exceed 100 characters")
        String sipUsername,

        @NotBlank(message = "SIP password is required")
        String sipPassword,

        @Size(max = 100, message = "Display name must not exceed 100 characters")
        String displayName

) {}
