package be.steby.CoreProject.bll.domains.crm.supportticket.models;

/**
 * BLL request model for assigning a support ticket to a team member.
 *
 * <p>Passing {@code null} as {@code assignedToPublicId} removes the current assignee.</p>
 *
 * @param assignedToPublicId public UUID of the user to assign the ticket to, or {@code null} to unassign
 */
public record SupportTicketAssignRequest(
        String assignedToPublicId
) {}
