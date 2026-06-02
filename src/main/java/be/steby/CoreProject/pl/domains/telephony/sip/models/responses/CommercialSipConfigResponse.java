package be.steby.CoreProject.pl.domains.telephony.sip.models.responses;

import be.steby.CoreProject.dl.entities.crm.CommercialSipConfig;

import java.time.Instant;

/**
 * Response model for a commercial SIP config. Password is never included.
 *
 * @param publicId    public UUID of the config
 * @param userPublicId public UUID of the commercial it belongs to
 * @param sipUsername SIP extension username
 * @param displayName optional SIP display name
 * @param createdAt   creation timestamp
 * @param updatedAt   last update timestamp
 */
public record CommercialSipConfigResponse(
        String publicId,
        String userPublicId,
        String sipUsername,
        String displayName,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommercialSipConfigResponse fromEntity(CommercialSipConfig c) {
        return new CommercialSipConfigResponse(
                c.getPublicId(),
                c.getUser().getPublicId(),
                c.getSipUsername(),
                c.getDisplayName(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
