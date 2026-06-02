package be.steby.CoreProject.pl.domains.telephony.admin.models.responses;

import java.time.Instant;

/**
 * Admin view of a user's telephony configuration.
 *
 * <p>{@code effectiveProvider} is the provider actually used for this user:
 * SIP if a {@link be.steby.CoreProject.dl.entities.crm.CommercialSipConfig} exists,
 * TWILIO if global active config is Twilio and no SIP override, NONE otherwise.</p>
 *
 * @param effectiveProvider derived provider for this user: SIP | TWILIO | NONE
 * @param globalProvider    active global provider: SIP | TWILIO | TEL_URI
 * @param sipConfig         non-null when the user has a SIP config assigned
 * @param twilioIdentity    publicId of the user (auto-identity) when effective = TWILIO
 */
public record AdminUserTelephonyStatusResponse(
        String effectiveProvider,
        String globalProvider,
        SipConfigSummary sipConfig,
        String twilioIdentity
) {

    /**
     * Minimal SIP config info exposed to the admin (no password).
     *
     * @param publicId    config public UUID (needed for update / delete operations)
     * @param sipUsername SIP extension username on Asterisk
     * @param displayName optional SIP display name
     * @param updatedAt   last modification timestamp
     */
    public record SipConfigSummary(
            String publicId,
            String sipUsername,
            String displayName,
            Instant updatedAt
    ) {}
}
