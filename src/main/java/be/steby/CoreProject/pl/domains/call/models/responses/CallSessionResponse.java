package be.steby.CoreProject.pl.domains.call.models.responses;

import be.steby.CoreProject.dl.entities.crm.CallSession;
import be.steby.CoreProject.dl.enums.crm.CallProvider;
import be.steby.CoreProject.dl.enums.crm.CallSessionStatus;

import java.time.Instant;

/**
 * Response model for a CRM call session.
 *
 * @param publicId            public UUID of the session
 * @param provider            telephony provider that handled the call
 * @param phoneNumber         phone number that was dialled
 * @param status              current lifecycle status of the session
 * @param startedAt           when the call was initiated
 * @param answeredAt          when the remote party answered; {@code null} if not answered
 * @param endedAt             when the call ended; {@code null} while in progress
 * @param durationSeconds     connected duration in seconds; {@code null} if not answered
 * @param contactPublicId     public UUID of the linked contact; {@code null} if linked to a lead
 * @param leadPublicId        public UUID of the linked lead; {@code null} if linked to a contact
 * @param performedByPublicId public UUID of the commercial who placed the call
 * @param createdAt           timestamp of entity creation
 */
public record CallSessionResponse(
        String publicId,
        CallProvider provider,
        String phoneNumber,
        CallSessionStatus status,
        Instant startedAt,
        Instant answeredAt,
        Instant endedAt,
        Integer durationSeconds,
        String contactPublicId,
        String leadPublicId,
        String performedByPublicId,
        Instant createdAt
) {

    /**
     * Maps a {@link CallSession} entity to a {@link CallSessionResponse}.
     *
     * @param s the call session entity
     * @return the response DTO
     */
    public static CallSessionResponse fromEntity(CallSession s) {
        return new CallSessionResponse(
                s.getPublicId(),
                s.getProvider(),
                s.getPhoneNumber(),
                s.getStatus(),
                s.getStartedAt(),
                s.getAnsweredAt(),
                s.getEndedAt(),
                s.getDurationSeconds(),
                s.getContact()     != null ? s.getContact().getPublicId()     : null,
                s.getLead()        != null ? s.getLead().getPublicId()         : null,
                s.getPerformedBy() != null ? s.getPerformedBy().getPublicId() : null,
                s.getCreatedAt()
        );
    }
}
