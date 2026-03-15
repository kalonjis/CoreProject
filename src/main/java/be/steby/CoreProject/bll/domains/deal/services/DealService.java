package be.steby.CoreProject.bll.domains.deal.services;

import be.steby.CoreProject.bll.domains.deal.exceptions.DealAlreadyClosedException;
import be.steby.CoreProject.bll.domains.deal.exceptions.DealNotFoundException;
import be.steby.CoreProject.bll.domains.deal.exceptions.DealStageNotInPipelineException;
import be.steby.CoreProject.bll.domains.deal.models.DealCreateRequest;
import be.steby.CoreProject.bll.domains.deal.models.DealFilterRequest;
import be.steby.CoreProject.bll.domains.deal.models.DealReassignRequest;
import be.steby.CoreProject.bll.domains.deal.models.DealUpdateRequest;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Deal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service for managing deals throughout their full CRM pipeline lifecycle.
 *
 * <h3>Deal lifecycle</h3>
 * <pre>
 * OPEN ──► (moves through pipeline stages) ──► WON  ✅  → closedAt set
 *                                          └──► LOST ❌  → closedAt set
 * </pre>
 *
 * <h3>Stage transitions</h3>
 * <p>Stage moves are validated by the service layer — the target stage must belong
 * to the deal's current pipeline. Terminal stages ({@code isWon} or {@code isLost})
 * automatically close the deal and publish the appropriate domain event.</p>
 *
 * <h3>Closed deal guard</h3>
 * <p>Update and stage-move operations are blocked on closed deals
 * ({@code WON} or {@code LOST}). Reassignment is allowed on closed deals.</p>
 */
public interface DealService {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds a deal by its internal database ID.
     *
     * <p><strong>Internal use only</strong> — intended for cross-domain service
     * calls and listeners where the internal ID is already known via JPA
     * relationships. Never expose this ID via API.</p>
     *
     * @param id the internal database ID
     * @return the matching deal
     * @throws DealNotFoundException if not found
     */
    Deal getById(Long id);

    /**
     * Finds a deal by its public UUID.
     *
     * @param publicId the public UUID of the deal
     * @return the matching deal
     * @throws DealNotFoundException if not found
     */
    Deal getByPublicId(String publicId);

    /**
     * Returns a paginated, filtered list of deals.
     *
     * <p>All filter fields in {@link DealFilterRequest} are optional.
     * Criteria are combined with AND logic via {@code DealSpecification}.</p>
     *
     * @param filter   the filter criteria (all fields optional)
     * @param pageable pagination and sorting parameters
     * @return a page of matching deals
     */
    Page<Deal> findAll(DealFilterRequest filter, Pageable pageable);

    /**
     * Returns all deals linked to a given contact.
     *
     * @param contactPublicId the public UUID of the contact
     * @return list of deals for that contact (may be empty)
     */
    List<Deal> findByContact(String contactPublicId);

    /**
     * Returns all deals linked to a given organisation.
     *
     * @param organisationPublicId the public UUID of the organisation
     * @return list of deals for that organisation (may be empty)
     */
    List<Deal> findByOrganisation(String organisationPublicId);

    // =========================================================================
    // Creation & update
    // =========================================================================

    /**
     * Creates a deal manually, linking it to a pipeline, contact, and commercial.
     *
     * <p>The stage must belong to the specified pipeline. Organisation is optional.
     * Deal status defaults to {@code OPEN}.</p>
     *
     * <p>Publishes a {@code DealCreatedEvent} on success.</p>
     *
     * @param request the deal creation data
     * @param actor   the commercial performing the operation
     * @return the newly created deal
     * @throws DealStageNotInPipelineException
     *         if the provided stage does not belong to the provided pipeline
     */
    Deal create(DealCreateRequest request, User actor);

    /**
     * Partially updates an existing deal's editable fields.
     *
     * <p>Only non-null fields in {@link DealUpdateRequest} are applied.
     * Null fields are ignored and existing values are preserved.</p>
     *
     * <p>Fields managed by dedicated operations, excluded here:</p>
     * <ul>
     *   <li>{@code status} / {@code pipelineStep} — use {@link #moveToStage}</li>
     *   <li>{@code assignedTo} — use {@link #reassign}</li>
     *   <li>{@code contact} — immutable after creation</li>
     *   <li>{@code closedAt} — set automatically on terminal stage entry</li>
     * </ul>
     *
     * <p>Publishes a {@code DealUpdatedEvent} on success.</p>
     *
     * @param publicId the public UUID of the deal to update
     * @param request  the partial update request
     * @param actor    the user performing the update
     * @return the updated deal
     * @throws DealNotFoundException       if not found
     * @throws DealAlreadyClosedException  if the deal is already closed
     */
    Deal update(String publicId, DealUpdateRequest request, User actor);

    // =========================================================================
    // Stage & lifecycle
    // =========================================================================

    /**
     * Moves a deal to a new pipeline stage.
     *
     * <p>Validates that the target stage belongs to the deal's current pipeline.
     * If the new stage is terminal:</p>
     * <ul>
     *   <li>{@code isWon = true} → sets status to {@code WON}, records {@code closedAt},
     *       publishes {@code DealWonEvent} and {@code DealStageChangedEvent}</li>
     *   <li>{@code isLost = true} → sets status to {@code LOST}, records {@code closedAt},
     *       publishes {@code DealLostEvent} and {@code DealStageChangedEvent}</li>
     *   <li>Non-terminal → publishes {@code DealStageChangedEvent} only</li>
     * </ul>
     *
     * @param publicId      the public UUID of the deal
     * @param stagePublicId the public UUID of the target pipeline step
     * @param actor         the user performing the stage move
     * @return the updated deal
     * @throws DealNotFoundException            if not found
     * @throws DealAlreadyClosedException       if the deal is already closed
     * @throws DealStageNotInPipelineException  if the stage does not belong to the deal's pipeline
     */
    Deal moveToStage(String publicId, String stagePublicId, User actor);

    // =========================================================================
    // Assignment
    // =========================================================================

    /**
     * Reassigns a deal to a different commercial (or unassigns it).
     *
     * <p>Passing {@code null} as {@code request.assignedToPublicId()} removes the
     * current assignee. Publishes a {@code DealReassignedEvent} on success.</p>
     *
     * @param publicId the public UUID of the deal
     * @param request  the reassignment request (assignedToPublicId may be null to unassign)
     * @param actor    the user performing the operation
     * @return the updated deal
     * @throws DealNotFoundException if not found
     */
    Deal reassign(String publicId, DealReassignRequest request, User actor);
}
