package be.steby.CoreProject.bll.domains.commercialaction.services;

import be.steby.CoreProject.bll.domains.commercialaction.exceptions.CommercialActionAlreadyTerminatedException;
import be.steby.CoreProject.bll.domains.commercialaction.exceptions.CommercialActionNotFoundException;
import be.steby.CoreProject.bll.domains.commercialaction.exceptions.CommercialActionValidationException;
import be.steby.CoreProject.bll.domains.commercialaction.models.CommercialActionCreateRequest;
import be.steby.CoreProject.bll.domains.commercialaction.models.CommercialActionFilterRequest;
import be.steby.CoreProject.bll.domains.commercialaction.models.CommercialActionUpdateRequest;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CommercialAction;
import be.steby.CoreProject.dl.enums.crm.CommercialActionStatus;

import java.util.List;

/**
 * Service for managing {@link CommercialAction} tasks throughout their lifecycle.
 *
 * <h3>Action lifecycle</h3>
 * <pre>
 * PENDING ──► DONE      ✅  → completedAt set
 *         └──► CANCELLED ❌  → terminal, no completedAt
 * </pre>
 *
 * <h3>Terminal state guard</h3>
 * <p>Once a commercial action reaches {@code DONE} or {@code CANCELLED}, it cannot
 * be transitioned again. Update and lifecycle operations are blocked on terminal
 * actions with a {@link CommercialActionAlreadyTerminatedException}.</p>
 *
 * <h3>Reassignment</h3>
 * <p>Reassignment can be performed either as part of an update (via
 * {@link CommercialActionUpdateRequest#assignedToPublicId()}) or as a dedicated
 * operation via {@link #reassign(String, String, User)}.</p>
 */
public interface CommercialActionService {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds a commercial action by its public UUID.
     *
     * @param publicId the public UUID of the action
     * @return the matching commercial action
     * @throws CommercialActionNotFoundException if not found
     */
    CommercialAction getByPublicId(String publicId);

    /**
     * Returns all commercial actions linked to a given deal, ordered by due date ascending.
     *
     * @param dealPublicId the public UUID of the deal
     * @return list of actions for that deal (may be empty)
     */
    List<CommercialAction> findByDeal(String dealPublicId);

    /**
     * Returns all commercial actions linked to a given contact, ordered by due date ascending.
     *
     * @param contactPublicId the public UUID of the contact
     * @return list of actions for that contact (may be empty)
     */
    List<CommercialAction> findByContact(String contactPublicId);

    /**
     * Returns all commercial actions assigned to a given commercial filtered by status,
     * ordered by due date ascending.
     *
     * <p>If {@code status} is {@code null}, defaults to {@code PENDING}.</p>
     *
     * @param assignedToPublicId the public UUID of the assigned commercial
     * @param status             the status filter; if null, defaults to {@code PENDING}
     * @return list of matching actions (may be empty)
     */
    List<CommercialAction> findByAssignedTo(String assignedToPublicId, CommercialActionStatus status);

    // =========================================================================
    // Write
    // =========================================================================

    /**
     * Creates a commercial action, linking it to at least one of a deal or contact.
     *
     * <p>Business rules enforced:</p>
     * <ul>
     *   <li>At least one of {@code dealPublicId} or {@code contactPublicId} must be set</li>
     *   <li>{@code assignedToPublicId} is required</li>
     *   <li>Priority defaults to {@code MEDIUM} if not specified</li>
     * </ul>
     *
     * <p>Publishes a {@code CommercialActionCreatedEvent} on success.</p>
     *
     * @param request the action creation data
     * @param actor   the commercial performing the operation
     * @return the newly created commercial action
     * @throws CommercialActionValidationException if business validation fails
     */
    CommercialAction create(CommercialActionCreateRequest request, User actor);

    /**
     * Partially updates an existing commercial action's editable fields.
     *
     * <p>Only non-null fields in {@link CommercialActionUpdateRequest} are applied.
     * Null fields are ignored and existing values are preserved.</p>
     *
     * <p>If {@code assignedToPublicId} is non-null, the action is also reassigned
     * and a {@code CommercialActionReassignedEvent} is published.</p>
     *
     * <p>Publishes a {@code CommercialActionUpdatedEvent} on success.</p>
     *
     * @param publicId the public UUID of the action to update
     * @param request  the partial update request
     * @param actor    the user performing the update
     * @return the updated commercial action
     * @throws CommercialActionNotFoundException         if not found
     * @throws CommercialActionAlreadyTerminatedException if the action is already terminal
     */
    CommercialAction update(String publicId, CommercialActionUpdateRequest request, User actor);

    /**
     * Marks a commercial action as {@code DONE} and records the completion timestamp.
     *
     * <p>Publishes a {@code CommercialActionCompletedEvent} on success.</p>
     *
     * @param publicId the public UUID of the action to complete
     * @param actor    the user performing the operation
     * @return the updated commercial action
     * @throws CommercialActionNotFoundException         if not found
     * @throws CommercialActionAlreadyTerminatedException if the action is not in {@code PENDING} status
     */
    CommercialAction complete(String publicId, User actor);

    /**
     * Marks a commercial action as {@code CANCELLED}.
     *
     * <p>Publishes a {@code CommercialActionCancelledEvent} on success.</p>
     *
     * @param publicId the public UUID of the action to cancel
     * @param actor    the user performing the operation
     * @return the updated commercial action
     * @throws CommercialActionNotFoundException         if not found
     * @throws CommercialActionAlreadyTerminatedException if the action is not in {@code PENDING} status
     */
    CommercialAction cancel(String publicId, User actor);

    /**
     * Reassigns a commercial action to a different commercial.
     *
     * <p>Publishes a {@code CommercialActionReassignedEvent} on success.</p>
     *
     * @param publicId              the public UUID of the action
     * @param newAssigneePublicId   the public UUID of the new assignee
     * @param actor                 the user performing the operation
     * @return the updated commercial action
     * @throws CommercialActionNotFoundException if not found
     */
    CommercialAction reassign(String publicId, String newAssigneePublicId, User actor);
}
