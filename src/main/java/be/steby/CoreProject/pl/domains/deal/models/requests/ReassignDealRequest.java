package be.steby.CoreProject.pl.domains.deal.models.requests;

import be.steby.CoreProject.bll.domains.deal.models.DealReassignRequest;

/**
 * PL request model for reassigning a deal to a different commercial.
 *
 * <h3>Unassignment</h3>
 * <p>Passing {@code null} as {@code assignedToPublicId} removes the current
 * assignee, leaving the deal unassigned.</p>
 *
 * @param assignedToPublicId public UUID of the new assignee, or {@code null} to unassign
 */
public record ReassignDealRequest(
        String assignedToPublicId
) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link DealReassignRequest} for the service layer
     */
    public DealReassignRequest toBllModel() {
        return new DealReassignRequest(assignedToPublicId);
    }
}
