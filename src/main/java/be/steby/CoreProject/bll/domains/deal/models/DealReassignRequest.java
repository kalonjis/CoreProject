package be.steby.CoreProject.bll.domains.deal.models;

/**
 * BLL request model for reassigning a {@link be.steby.CoreProject.dl.entities.crm.Deal}
 * to a different commercial.
 *
 * <h3>Unassignment</h3>
 * <p>Passing {@code null} as {@code assignedToPublicId} removes the current
 * assignee, leaving the deal unassigned until it is explicitly reassigned.</p>
 *
 * @param assignedToPublicId public UUID of the new assignee, or {@code null} to unassign
 */
public record DealReassignRequest(
        String assignedToPublicId
) {}
