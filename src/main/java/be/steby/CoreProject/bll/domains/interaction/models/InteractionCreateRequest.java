package be.steby.CoreProject.bll.domains.interaction.models;

import be.steby.CoreProject.dl.enums.crm.CallStatus;
import be.steby.CoreProject.dl.enums.crm.InteractionDirection;
import be.steby.CoreProject.dl.enums.crm.InteractionOutcome;
import be.steby.CoreProject.dl.enums.crm.InteractionType;

import java.time.Instant;

/**
 * BLL request model for logging a new {@link be.steby.CoreProject.dl.entities.crm.Interaction}.
 *
 * <p>Used when a commercial records a touchpoint against a deal or contact.
 * At least one of {@code dealPublicId} or {@code contactPublicId} must be provided —
 * this constraint is enforced at the service layer.</p>
 *
 * <h3>Type-specific detail records</h3>
 * <ul>
 *   <li>{@code type == CALL}  → {@code callLogDetails} must be non-null, {@code emailLogDetails} must be null</li>
 *   <li>{@code type == EMAIL} → {@code emailLogDetails} must be non-null, {@code callLogDetails} must be null</li>
 *   <li>Other types           → both detail records must be null</li>
 * </ul>
 *
 * @param type             type of interaction (required)
 * @param direction        direction relative to the company — OUTBOUND or INBOUND (optional)
 * @param subject          short subject or title of the interaction (required)
 * @param notes            free-text summary written by the commercial (optional)
 * @param outcome          outcome classification — POSITIVE, NEUTRAL, NEGATIVE, NO_ANSWER (optional)
 * @param durationMinutes  duration of the interaction in minutes (optional)
 * @param occurredAt       when the interaction actually took place (required)
 * @param dealPublicId     public UUID of the linked deal (optional — at least one of deal/contact required)
 * @param contactPublicId  public UUID of the linked contact (optional — at least one of deal/contact required)
 * @param callLogDetails   structured call details — required when type is CALL (optional otherwise)
 * @param emailLogDetails  structured email details — required when type is EMAIL (optional otherwise)
 */
public record InteractionCreateRequest(
        InteractionType type,
        InteractionDirection direction,
        String subject,
        String notes,
        InteractionOutcome outcome,
        Integer durationMinutes,
        Instant occurredAt,
        String dealPublicId,
        String contactPublicId,
        CallLogDetails callLogDetails,
        EmailLogDetails emailLogDetails
) {

    // =========================================================================
    // Nested detail records
    // =========================================================================

    /**
     * Structured details for a phone call interaction.
     *
     * <p>Required when {@code type == CALL}. Contains all call-specific
     * metadata that is stored in the {@link be.steby.CoreProject.dl.entities.crm.CallLog}
     * sub-entity.</p>
     *
     * @param phoneNumber     phone number dialled or received from (optional)
     * @param durationSeconds actual call duration in seconds (optional — null if not answered)
     * @param status          outcome status of the call attempt (required)
     * @param recordingUrl    URL to the call recording if available (optional)
     */
    public record CallLogDetails(
            String phoneNumber,
            Integer durationSeconds,
            CallStatus status,
            String recordingUrl
    ) {}

    /**
     * Structured details for an email interaction.
     *
     * <p>Required when {@code type == EMAIL}. Contains all email-specific
     * metadata that is stored in the {@link be.steby.CoreProject.dl.entities.crm.EmailLog}
     * sub-entity.</p>
     *
     * @param emailSubject       subject line of the email (required)
     * @param bodySnippet        short excerpt of the email body (optional)
     * @param externalMessageId  identifier assigned by the mail provider (optional)
     */
    public record EmailLogDetails(
            String emailSubject,
            String bodySnippet,
            String externalMessageId
    ) {}
}
