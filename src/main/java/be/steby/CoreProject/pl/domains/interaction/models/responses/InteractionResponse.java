package be.steby.CoreProject.pl.domains.interaction.models.responses;

import be.steby.CoreProject.dl.entities.crm.CallLog;
import be.steby.CoreProject.dl.entities.crm.EmailLog;
import be.steby.CoreProject.dl.entities.crm.Interaction;
import be.steby.CoreProject.dl.enums.crm.CallStatus;
import be.steby.CoreProject.dl.enums.crm.InteractionDirection;
import be.steby.CoreProject.dl.enums.crm.InteractionOutcome;
import be.steby.CoreProject.dl.enums.crm.InteractionType;

import java.time.Instant;

/**
 * Response model for a CRM interaction.
 *
 * <p>Represents the full detail of an interaction, including type-specific
 * call or email log data when present. Used for both single-record retrieval
 * and timeline list endpoints.</p>
 *
 * @param publicId            public UUID of the interaction
 * @param type                type of action (CALL, EMAIL, MEETING, NOTE, VISIT, ACTION_DONE)
 * @param direction           direction relative to the company, or {@code null} if not applicable
 * @param subject             short subject or title of the interaction
 * @param notes               free-text summary, or {@code null} if none
 * @param outcome             outcome classification, or {@code null} if not set
 * @param durationMinutes     duration in minutes, or {@code null} if not set
 * @param occurredAt          when the interaction took place
 * @param dealPublicId        public UUID of the linked deal, or {@code null}
 * @param contactPublicId     public UUID of the linked contact, or {@code null}
 * @param performedByPublicId public UUID of the commercial who performed the interaction
 * @param performedByUsername username of the commercial who performed the interaction
 * @param callLog             structured call details, or {@code null} if not a CALL type
 * @param emailLog            structured email details, or {@code null} if not an EMAIL type
 * @param createdAt           timestamp of entity creation
 * @param updatedAt           timestamp of last update
 */
public record InteractionResponse(
        String publicId,
        InteractionType type,
        InteractionDirection direction,
        String subject,
        String notes,
        InteractionOutcome outcome,
        Integer durationMinutes,
        Instant occurredAt,
        String dealPublicId,
        String contactPublicId,
        String performedByPublicId,
        String performedByUsername,
        CallLogResponse callLog,
        EmailLogResponse emailLog,
        Instant createdAt,
        Instant updatedAt
) {

    // =========================================================================
    // Nested response records
    // =========================================================================

    /**
     * Response model for call-specific details.
     *
     * <p>Present only when the parent interaction has {@code type == CALL}.</p>
     *
     * @param phoneNumber     phone number dialled or received from, or {@code null}
     * @param durationSeconds call duration in seconds, or {@code null} if not answered
     * @param status          outcome status of the call attempt
     * @param recordingUrl    URL to the call recording, or {@code null} if not available
     */
    public record CallLogResponse(
            String phoneNumber,
            Integer durationSeconds,
            CallStatus status,
            String recordingUrl
    ) {}

    /**
     * Response model for email-specific details.
     *
     * <p>Present only when the parent interaction has {@code type == EMAIL}.</p>
     *
     * @param emailSubject       subject line of the email
     * @param bodySnippet        short excerpt of the email body, or {@code null}
     * @param externalMessageId  identifier assigned by the mail provider, or {@code null}
     * @param openedAt           timestamp when the email was first opened, or {@code null}
     * @param clickedAt          timestamp when a tracked link was first clicked, or {@code null}
     * @param wasOpened          {@code true} if the email has been opened
     * @param wasClicked         {@code true} if a tracked link was clicked
     */
    public record EmailLogResponse(
            String emailSubject,
            String bodySnippet,
            String externalMessageId,
            Instant openedAt,
            Instant clickedAt,
            boolean wasOpened,
            boolean wasClicked
    ) {}

    // =========================================================================
    // Static factory
    // =========================================================================

    /**
     * Maps an {@link Interaction} entity to an {@link InteractionResponse}.
     *
     * @param i the interaction entity
     * @return the full interaction response
     */
    public static InteractionResponse fromEntity(Interaction i) {
        return new InteractionResponse(
                i.getPublicId(),
                i.getType(),
                i.getDirection(),
                i.getSubject(),
                i.getNotes(),
                i.getOutcome(),
                i.getDurationMinutes(),
                i.getOccurredAt(),
                i.getDeal()        != null ? i.getDeal().getPublicId()        : null,
                i.getContact()     != null ? i.getContact().getPublicId()     : null,
                i.getPerformedBy() != null ? i.getPerformedBy().getPublicId() : null,
                i.getPerformedBy() != null ? i.getPerformedBy().getUsername() : null,
                mapCallLog(i.getCallLog()),
                mapEmailLog(i.getEmailLog()),
                i.getCreatedAt(),
                i.getUpdatedAt()
        );
    }

    // =========================================================================
    // Private mapping helpers
    // =========================================================================

    private static CallLogResponse mapCallLog(CallLog callLog) {
        if (callLog == null) return null;
        return new CallLogResponse(
                callLog.getPhoneNumber(),
                callLog.getDurationSeconds(),
                callLog.getStatus(),
                callLog.getRecordingUrl()
        );
    }

    private static EmailLogResponse mapEmailLog(EmailLog emailLog) {
        if (emailLog == null) return null;
        return new EmailLogResponse(
                emailLog.getSubject(),
                emailLog.getBodySnippet(),
                emailLog.getExternalMessageId(),
                emailLog.getOpenedAt(),
                emailLog.getClickedAt(),
                emailLog.wasOpened(),
                emailLog.wasClicked()
        );
    }
}
