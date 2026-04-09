package be.steby.CoreProject.bll.domains.crm.contact.models;

/**
 * BLL request model for assigning a {@link be.steby.CoreProject.dl.entities.crm.Contact}
 * to a commercial.
 *
 * <p>Assignment is intentionally a separate operation from creation and update —
 * it is a distinct business action that publishes its own domain event
 * ({@code ContactAssignedEvent}) and may trigger notifications.</p>
 *
 * <h3>Unassignment</h3>
 * <p>Passing {@code null} as {@code commercialPublicId} removes the current
 * assignee. The service layer is responsible for enforcing any business rules
 * around unassignment (e.g., requiring a reason).</p>
 *
 * @param commercialPublicId public UUID of the commercial to assign,
 *                           or {@code null} to unassign
 */
public record ContactAssignRequest(
        String commercialPublicId
) {}