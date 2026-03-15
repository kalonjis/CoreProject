package be.steby.CoreProject.pl.domains.commercialaction.models.requests;

/**
 * PL request model for reassigning a commercial action to a different commercial.
 *
 * @param assignedToPublicId public UUID of the new assignee (required)
 */
public record ReassignCommercialActionRequest(
        String assignedToPublicId
) {}
