package be.steby.CoreProject.pl.domains.supportticket.models.requests;

import be.steby.CoreProject.bll.domains.supportticket.models.SupportTicketAssignRequest;

/**
 * PL request model for assigning a support ticket to a team member.
 *
 * <p>Pass {@code null} as {@code assignedToPublicId} to unassign the ticket.</p>
 *
 * @param assignedToPublicId public UUID of the user to assign to, or {@code null} to unassign
 */
public record AssignSupportTicketRequest(
        String assignedToPublicId
) {
    public SupportTicketAssignRequest toBllModel() {
        return new SupportTicketAssignRequest(assignedToPublicId);
    }
}
