package be.steby.CoreProject.pl.domains.supportticket.models.requests;

import be.steby.CoreProject.bll.domains.crm.supportticket.models.SupportTicketChangeStatusRequest;
import be.steby.CoreProject.dl.enums.crm.SupportTicketStatus;
import jakarta.validation.constraints.NotNull;

/**
 * PL request model for changing a support ticket's lifecycle status.
 *
 * @param status the target status (required)
 */
public record ChangeSupportTicketStatusRequest(

        @NotNull(message = "Status is required")
        SupportTicketStatus status

) {
    public SupportTicketChangeStatusRequest toBllModel() {
        return new SupportTicketChangeStatusRequest(status);
    }
}
