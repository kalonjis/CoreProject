package be.steby.CoreProject.pl.domains.call.models.requests;

import be.steby.CoreProject.bll.domains.crm.call.models.TerminateCallRequest;
import be.steby.CoreProject.dl.enums.crm.CallSessionStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * PL request model for terminating an active call session.
 *
 * <p>{@code status} must be a terminal value ({@code ENDED}, {@code MISSED},
 * or {@code FAILED}). The service layer enforces this constraint.</p>
 *
 * @param status          the terminal status of the call (required)
 * @param durationSeconds connected duration in seconds; {@code null} for missed or failed calls
 */
public record TerminateCallHttpRequest(

        @NotNull(message = "Call status is required")
        CallSessionStatus status,

        @Min(value = 0, message = "Duration must be zero or positive")
        Integer durationSeconds

) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link TerminateCallRequest} for the service layer
     */
    public TerminateCallRequest toBllModel() {
        return new TerminateCallRequest(status, durationSeconds);
    }
}
