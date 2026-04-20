package be.steby.CoreProject.pl.domains.commercialaction.models.requests;

import be.steby.CoreProject.bll.domains.crm.commercialaction.models.CommercialActionCompleteRequest;
import be.steby.CoreProject.bll.domains.crm.interaction.models.InteractionCreateRequest;
import be.steby.CoreProject.dl.enums.crm.CallStatus;
import be.steby.CoreProject.dl.enums.crm.InteractionOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Optional request body for completing a {@link be.steby.CoreProject.dl.entities.crm.CommercialAction}.
 *
 * <p>Supply the details record that matches the action type:
 * {@code callLogDetails} for {@code CALL}, {@code emailLogDetails} for {@code EMAIL},
 * {@code meetingLogDetails} for {@code MEETING} or {@code DEMO}.
 * For {@code TASK} the body may be omitted entirely.</p>
 */
public record CompleteCommercialActionRequest(
        @Valid CallLogDetails callLogDetails,
        @Valid EmailLogDetails emailLogDetails,
        @Valid MeetingLogDetails meetingLogDetails
) {

    public record CallLogDetails(
            @NotNull CallStatus status,
            Integer durationSeconds,
            String phoneNumber
    ) {}

    public record EmailLogDetails(
            @NotBlank String emailSubject,
            String bodySnippet,
            String externalMessageId
    ) {}

    public record MeetingLogDetails(
            InteractionOutcome outcome,
            String notes
    ) {}

    public CommercialActionCompleteRequest toBllModel() {
        InteractionCreateRequest.CallLogDetails callDetails = callLogDetails != null
                ? new InteractionCreateRequest.CallLogDetails(
                        callLogDetails.phoneNumber(),
                        callLogDetails.durationSeconds(),
                        callLogDetails.status(),
                        null)
                : null;

        InteractionCreateRequest.EmailLogDetails emailDetails = emailLogDetails != null
                ? new InteractionCreateRequest.EmailLogDetails(
                        emailLogDetails.emailSubject(),
                        emailLogDetails.bodySnippet(),
                        emailLogDetails.externalMessageId())
                : null;

        CommercialActionCompleteRequest.MeetingLogDetails meetingDetails = meetingLogDetails != null
                ? new CommercialActionCompleteRequest.MeetingLogDetails(
                        meetingLogDetails.outcome(),
                        meetingLogDetails.notes())
                : null;

        return new CommercialActionCompleteRequest(callDetails, emailDetails, meetingDetails);
    }
}
