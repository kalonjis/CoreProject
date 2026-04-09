package be.steby.CoreProject.bll.domains.commercialaction.models;

import be.steby.CoreProject.bll.domains.crm.interaction.models.InteractionCreateRequest;
import be.steby.CoreProject.dl.enums.crm.InteractionOutcome;

/**
 * Optional completion details for a {@link be.steby.CoreProject.dl.entities.crm.CommercialAction}.
 *
 * <p>At most one of the detail fields should be non-null at a time.
 * Validation of that constraint is delegated to the listener.</p>
 *
 * @param callLogDetails    structured call details — provided when type is {@code CALL}
 * @param emailLogDetails   structured email details — provided when type is {@code EMAIL}
 * @param meetingLogDetails meeting/demo outcome details — provided when type is {@code MEETING} or {@code DEMO}
 */
public record CommercialActionCompleteRequest(
        InteractionCreateRequest.CallLogDetails callLogDetails,
        InteractionCreateRequest.EmailLogDetails emailLogDetails,
        MeetingLogDetails meetingLogDetails
) {

    /**
     * Outcome details captured when a {@code MEETING} or {@code DEMO} action is completed.
     *
     * @param outcome  how the meeting went (optional)
     * @param notes    free-text summary written by the commercial (optional)
     */
    public record MeetingLogDetails(
            InteractionOutcome outcome,
            String notes
    ) {}
}
