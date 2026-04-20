package be.steby.CoreProject.pl.domains.supportticket.models.requests;

import be.steby.CoreProject.bll.domains.crm.supportticket.models.SupportTicketFilterRequest;
import be.steby.CoreProject.dl.enums.crm.SupportTicketSource;
import be.steby.CoreProject.dl.enums.crm.SupportTicketStatus;

/**
 * PL request model for filtering the support ticket list.
 *
 * <p>Bound from query parameters via {@code @ModelAttribute}.</p>
 *
 * @param keyword              keyword to search in subject (optional)
 * @param status               filter by lifecycle status (optional)
 * @param source               filter by creation source (optional)
 * @param contactPublicId      filter by submitting contact (optional)
 * @param assignedToPublicId   filter by assigned team member (optional)
 * @param unassignedOnly       if {@code true}, returns only unassigned tickets (optional)
 * @param organisationPublicId filter by the organisation of the submitting contact (optional)
 */
public record SupportTicketListFilterRequest(
        String keyword,
        SupportTicketStatus status,
        SupportTicketSource source,
        String contactPublicId,
        String assignedToPublicId,
        Boolean unassignedOnly,
        String organisationPublicId
) {
    public SupportTicketFilterRequest toBllModel() {
        return new SupportTicketFilterRequest(
                keyword,
                status,
                source,
                contactPublicId,
                assignedToPublicId,
                unassignedOnly,
                organisationPublicId
        );
    }
}
