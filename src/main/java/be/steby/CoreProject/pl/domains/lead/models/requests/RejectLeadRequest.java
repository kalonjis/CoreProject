package be.steby.CoreProject.pl.domains.lead.models.requests;

import be.steby.CoreProject.bll.domains.lead.models.LeadRejectRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PL request model for rejecting a lead.
 *
 * @param rejectionReason a short explanation of why the lead was rejected
 */
public record RejectLeadRequest(

        @NotBlank(message = "Rejection reason is required")
        @Size(max = 255, message = "Rejection reason must not exceed 255 characters")
        String rejectionReason

) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link LeadRejectRequest} for the service layer
     */
    public LeadRejectRequest toBllModel() {
        return new LeadRejectRequest(rejectionReason);
    }
}