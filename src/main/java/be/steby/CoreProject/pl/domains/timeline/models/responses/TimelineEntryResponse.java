package be.steby.CoreProject.pl.domains.timeline.models.responses;

import be.steby.CoreProject.bll.domains.crm.timeline.models.TimelineEntry;
import be.steby.CoreProject.dl.entities.crm.CallLog;
import be.steby.CoreProject.dl.entities.crm.CommercialAction;
import be.steby.CoreProject.dl.entities.crm.EmailLog;
import be.steby.CoreProject.dl.entities.crm.Interaction;
import be.steby.CoreProject.dl.enums.crm.CallStatus;
import be.steby.CoreProject.dl.enums.crm.CommercialActionPriority;
import be.steby.CoreProject.dl.enums.crm.CommercialActionType;
import be.steby.CoreProject.dl.enums.crm.InteractionDirection;
import be.steby.CoreProject.dl.enums.crm.InteractionOutcome;
import be.steby.CoreProject.dl.enums.crm.InteractionType;

import java.time.Instant;

/**
 * Unified response model for a CRM timeline entry.
 *
 * <p>Represents either a logged {@link Interaction} or a completed
 * {@link CommercialAction} as a single card in the timeline view.</p>
 *
 * <h3>Discriminator</h3>
 * <p>{@link #sourceType()} is always {@code "INTERACTION"} or {@code "COMMERCIAL_ACTION"}.
 * The frontend uses this field to select the appropriate card component and to
 * determine which type-specific fields are populated.</p>
 *
 * <h3>Shared fields (always present)</h3>
 * <ul>
 *   <li>{@link #publicId()}, {@link #subject()}, {@link #notes()}, {@link #occurredAt()}</li>
 *   <li>{@link #durationMinutes()}, {@link #dealPublicId()}, {@link #contactPublicId()}, {@link #leadPublicId()}</li>
 *   <li>{@link #performedByPublicId()}, {@link #performedByUsername()}</li>
 *   <li>{@link #createdAt()}, {@link #updatedAt()}</li>
 * </ul>
 *
 * <h3>INTERACTION-only fields</h3>
 * <ul>
 *   <li>{@link #interactionType()}, {@link #direction()}, {@link #outcome()}</li>
 *   <li>{@link #callLog()}, {@link #emailLog()}</li>
 * </ul>
 *
 * <h3>COMMERCIAL_ACTION-only fields</h3>
 * <ul>
 *   <li>{@link #actionType()}, {@link #priority()}, {@link #location()}</li>
 * </ul>
 */
public record TimelineEntryResponse(
        String sourceType,
        String publicId,
        String subject,
        String notes,
        Instant occurredAt,
        Integer durationMinutes,
        String dealPublicId,
        String contactPublicId,
        String leadPublicId,
        String performedByPublicId,
        String performedByUsername,

        // INTERACTION-only
        InteractionType interactionType,
        InteractionDirection direction,
        InteractionOutcome outcome,
        CallLogResponse callLog,
        EmailLogResponse emailLog,

        // COMMERCIAL_ACTION-only
        CommercialActionType actionType,
        CommercialActionPriority priority,
        String location,

        Instant createdAt,
        Instant updatedAt
) {

    // =========================================================================
    // Nested response records
    // =========================================================================

    public record CallLogResponse(
            String phoneNumber,
            Integer durationSeconds,
            CallStatus status,
            String recordingUrl
    ) {}

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

    public static TimelineEntryResponse fromEntry(TimelineEntry entry) {
        return switch (entry) {
            case TimelineEntry.InteractionEntry e      -> fromInteraction(e.interaction());
            case TimelineEntry.CommercialActionEntry e -> fromCommercialAction(e.action());
        };
    }

    // =========================================================================
    // Private mapping helpers
    // =========================================================================

    private static TimelineEntryResponse fromInteraction(Interaction i) {
        return new TimelineEntryResponse(
                "INTERACTION",
                i.getPublicId(),
                i.getSubject(),
                i.getNotes(),
                i.getOccurredAt(),
                i.getDurationMinutes(),
                i.getDeal()        != null ? i.getDeal().getPublicId()        : null,
                i.getContact()     != null ? i.getContact().getPublicId()     : null,
                i.getLead()        != null ? i.getLead().getPublicId()        : null,
                i.getPerformedBy() != null ? i.getPerformedBy().getPublicId() : null,
                i.getPerformedBy() != null ? i.getPerformedBy().getUsername() : null,
                i.getType(),
                i.getDirection(),
                i.getOutcome(),
                mapCallLog(i.getCallLog()),
                mapEmailLog(i.getEmailLog()),
                null, null, null,
                i.getCreatedAt(),
                i.getUpdatedAt()
        );
    }

    private static TimelineEntryResponse fromCommercialAction(CommercialAction a) {
        return new TimelineEntryResponse(
                "COMMERCIAL_ACTION",
                a.getPublicId(),
                a.getTitle(),
                a.getDescription(),
                a.getCompletedAt(),
                a.getDurationMinutes(),
                a.getDeal()       != null ? a.getDeal().getPublicId()       : null,
                a.getContact()    != null ? a.getContact().getPublicId()    : null,
                a.getLead()       != null ? a.getLead().getPublicId()       : null,
                a.getAssignedTo() != null ? a.getAssignedTo().getPublicId() : null,
                a.getAssignedTo() != null ? a.getAssignedTo().getUsername() : null,
                null, null, null, null, null,
                a.getType(),
                a.getPriority(),
                a.getLocation(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

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
