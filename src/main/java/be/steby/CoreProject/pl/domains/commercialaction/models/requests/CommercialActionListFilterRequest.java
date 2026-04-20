package be.steby.CoreProject.pl.domains.commercialaction.models.requests;

import be.steby.CoreProject.bll.domains.crm.commercialaction.models.CommercialActionFilterRequest;
import be.steby.CoreProject.dl.enums.crm.CommercialActionPriority;
import be.steby.CoreProject.dl.enums.crm.CommercialActionStatus;

/**
 * PL request model for filtering the commercial action list.
 *
 * <p>All fields are optional — passed as query parameters on list endpoints.
 * {@code null} means no restriction on that criterion.</p>
 *
 * @param assignedToPublicId filter actions assigned to a specific commercial
 * @param status             filter by lifecycle status
 * @param priority           filter by priority level
 * @param dealPublicId       filter actions linked to a specific deal
 * @param contactPublicId    filter actions linked to a specific contact
 */
public record CommercialActionListFilterRequest(

        String assignedToPublicId,
        CommercialActionStatus status,
        CommercialActionPriority priority,
        String dealPublicId,
        String contactPublicId

) {

    /**
     * Converts this PL request to the BLL filter model.
     *
     * @return {@link CommercialActionFilterRequest} for the service layer
     */
    public CommercialActionFilterRequest toBllModel() {
        return new CommercialActionFilterRequest(
                assignedToPublicId,
                status,
                priority,
                dealPublicId,
                contactPublicId
        );
    }
}
