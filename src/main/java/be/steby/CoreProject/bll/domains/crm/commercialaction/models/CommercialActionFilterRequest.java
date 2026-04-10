package be.steby.CoreProject.bll.domains.crm.commercialaction.models;

import be.steby.CoreProject.dl.enums.crm.CommercialActionPriority;
import be.steby.CoreProject.dl.enums.crm.CommercialActionStatus;

/**
 * BLL request model for filtering {@link be.steby.CoreProject.dl.entities.crm.CommercialAction}
 * entities in list queries.
 *
 * <p>All fields are optional. {@code null} fields are ignored — no filter is applied
 * on that criterion.</p>
 *
 * @param assignedToPublicId filter actions assigned to a specific commercial (optional)
 * @param status             filter by lifecycle status (optional)
 * @param priority           filter by priority level (optional)
 * @param dealPublicId       filter actions linked to a specific deal (optional)
 * @param contactPublicId    filter actions linked to a specific contact (optional)
 */
public record CommercialActionFilterRequest(
        String assignedToPublicId,
        CommercialActionStatus status,
        CommercialActionPriority priority,
        String dealPublicId,
        String contactPublicId
) {}
