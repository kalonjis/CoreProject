package be.steby.CoreProject.pl.domains.interaction.models.requests;

import be.steby.CoreProject.bll.domains.interaction.models.InteractionCreateRequest;
import be.steby.CoreProject.dl.enums.crm.CallStatus;
import be.steby.CoreProject.dl.enums.crm.InteractionDirection;
import be.steby.CoreProject.dl.enums.crm.InteractionOutcome;
import be.steby.CoreProject.dl.enums.crm.InteractionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * PL request model for logging a new CRM interaction.
 *
 * <p>At least one of {@code dealPublicId} or {@code contactPublicId} must be provided.
 * This constraint is enforced at the service layer rather than via Bean Validation
 * to produce a domain-specific error message.</p>
 *
 * <h3>Type-specific detail records</h3>
 * <ul>
 *   <li>{@code type == CALL}  → {@code callLog} must be non-null</li>
 *   <li>{@code type == EMAIL} → {@code emailLog} must be non-null</li>
 *   <li>Other types           → both should be null (service validates)</li>
 * </ul>
 *
 * @param type             type of interaction (required)
 * @param direction        direction relative to the company — OUTBOUND or INBOUND (optional)
 * @param subject          short subject or title of the interaction (required)
 * @param notes            free-text summary written by the commercial (optional)
 * @param outcome          outcome classification — POSITIVE, NEUTRAL, NEGATIVE, NO_ANSWER (optional)
 * @param durationMinutes  duration in minutes (optional)
 * @param occurredAt       when the interaction actually took place (required)
 * @param dealPublicId     public UUID of the linked deal (optional — at least one of lead/deal/contact required)
 * @param contactPublicId  public UUID of the linked contact (optional — at least one of lead/deal/contact required)
 * @param leadPublicId     public UUID of the linked lead (optional — at least one of lead/deal/contact required)
 * @param callLog          structured call details — required when type is CALL (optional otherwise)
 * @param emailLog         structured email details — required when type is EMAIL (optional otherwise)
 */
public record LogInteractionRequest(

        @NotNull(message = "Interaction type is required")
        InteractionType type,

        InteractionDirection direction,

        @NotBlank(message = "Subject is required")
        @Size(max = 200, message = "Subject must not exceed 200 characters")
        String subject,

        String notes,

        InteractionOutcome outcome,

        Integer durationMinutes,

        @NotNull(message = "Occurred-at timestamp is required")
        Instant occurredAt,

        String dealPublicId,

        String contactPublicId,

        String leadPublicId,

        CallLogRequest callLog,

        EmailLogRequest emailLog

) {

    // =========================================================================
    // Nested request records
    // =========================================================================

    /**
     * PL request details for a phone call interaction.
     *
     * <p>Required when {@code type == CALL}.</p>
     *
     * @param phoneNumber     phone number dialled or received from (optional)
     * @param durationSeconds call duration in seconds (optional)
     * @param status          outcome status of the call attempt (required)
     * @param recordingUrl    URL to the call recording if available (optional)
     */
    public record CallLogRequest(

            String phoneNumber,

            Integer durationSeconds,

            @NotNull(message = "Call status is required")
            CallStatus status,

            @Size(max = 500, message = "Recording URL must not exceed 500 characters")
            String recordingUrl

    ) {}

    /**
     * PL request details for an email interaction.
     *
     * <p>Required when {@code type == EMAIL}.</p>
     *
     * @param emailSubject       subject line of the email (required)
     * @param bodySnippet        short excerpt of the email body (optional)
     * @param externalMessageId  identifier assigned by the mail provider (optional)
     */
    public record EmailLogRequest(

            @NotBlank(message = "Email subject is required")
            @Size(max = 255, message = "Email subject must not exceed 255 characters")
            String emailSubject,

            @Size(max = 300, message = "Body snippet must not exceed 300 characters")
            String bodySnippet,

            @Size(max = 255, message = "External message ID must not exceed 255 characters")
            String externalMessageId

    ) {}

    // =========================================================================
    // BLL conversion
    // =========================================================================

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link InteractionCreateRequest} for the service layer
     */
    public InteractionCreateRequest toBllModel() {
        return new InteractionCreateRequest(
                type,
                direction,
                subject != null ? subject.trim() : null,
                notes,
                outcome,
                durationMinutes,
                occurredAt,
                dealPublicId,
                contactPublicId,
                leadPublicId,
                callLog != null
                        ? new InteractionCreateRequest.CallLogDetails(
                                callLog.phoneNumber(),
                                callLog.durationSeconds(),
                                callLog.status(),
                                callLog.recordingUrl())
                        : null,
                emailLog != null
                        ? new InteractionCreateRequest.EmailLogDetails(
                                emailLog.emailSubject(),
                                emailLog.bodySnippet(),
                                emailLog.externalMessageId())
                        : null
        );
    }
}
