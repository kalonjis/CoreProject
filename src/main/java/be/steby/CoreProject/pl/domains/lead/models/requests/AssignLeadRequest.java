package be.steby.CoreProject.pl.domains.lead.models.requests;

import be.steby.CoreProject.bll.domains.crm.lead.models.LeadAssignRequest;
import jakarta.validation.constraints.NotBlank;

/**
 * PL request model for assigning a lead to a commercial.
 *
 * @param commercialPublicId the public UUID of the commercial to assign the lead to
 */
public record AssignLeadRequest(

        @NotBlank(message = "Commercial public ID is required")
        String commercialPublicId

) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link LeadAssignRequest} for the service layer
     */
    public LeadAssignRequest toBllModel() {
        return new LeadAssignRequest(commercialPublicId);
    }
}