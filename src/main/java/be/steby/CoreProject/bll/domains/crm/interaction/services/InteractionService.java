package be.steby.CoreProject.bll.domains.crm.interaction.services;

import be.steby.CoreProject.bll.domains.crm.interaction.exceptions.InteractionNotFoundException;
import be.steby.CoreProject.bll.domains.crm.interaction.exceptions.InteractionValidationException;
import be.steby.CoreProject.bll.domains.crm.interaction.models.InteractionCreateRequest;
import be.steby.CoreProject.bll.domains.crm.interaction.models.InteractionUpdateRequest;
import be.steby.CoreProject.bll.domains.crm.timeline.services.TimelineService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Interaction;

/**
 * Service for managing CRM interactions (timeline touchpoints) across deals and contacts.
 *
 * <h3>Interaction model</h3>
 * <p>An {@link Interaction} records a single touchpoint in the CRM timeline — a call,
 * email, meeting, note, or visit. Every interaction must be linked to at least one
 * deal, contact, or lead, and always carries the commercial who performed it.</p>
 *
 * <h3>Type-specific sub-entities</h3>
 * <p>For structured data, {@code CALL} interactions carry a {@code CallLog} and
 * {@code EMAIL} interactions carry an {@code EmailLog}. These are created
 * automatically by {@link #create(InteractionCreateRequest, User)} when the
 * appropriate detail record is present in the request.</p>
 *
 * <h3>Unified timeline</h3>
 * <p>To retrieve the unified timeline that merges interactions with completed
 * commercial actions, use
 * {@link TimelineService}.</p>
 */
public interface InteractionService {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds an interaction by its public UUID.
     *
     * @param publicId the public UUID of the interaction
     * @return the matching interaction
     * @throws InteractionNotFoundException if not found
     */
    Interaction getByPublicId(String publicId);

    // =========================================================================
    // Write
    // =========================================================================

    /**
     * Logs a new interaction, creating the appropriate type-specific sub-entity
     * ({@code CallLog} or {@code EmailLog}) when applicable.
     *
     * <p>Business rules enforced:</p>
     * <ul>
     *   <li>At least one of {@code dealPublicId}, {@code contactPublicId}, or {@code leadPublicId} must be set</li>
     *   <li>{@code type == CALL} → {@code callLogDetails} required</li>
     *   <li>{@code type == EMAIL} → {@code emailLogDetails} required</li>
     * </ul>
     *
     * <p>Publishes an {@code InteractionCreatedEvent} on success.</p>
     *
     * @param request the interaction creation data
     * @param actor   the commercial performing the operation
     * @return the newly created interaction
     * @throws InteractionValidationException if business validation fails
     */
    Interaction create(InteractionCreateRequest request, User actor);

    /**
     * Partially updates an existing interaction's editable fields.
     *
     * <p>Only non-null fields in {@link InteractionUpdateRequest} are applied.
     * Null fields are ignored and existing values are preserved.</p>
     *
     * <p>Publishes an {@code InteractionUpdatedEvent} on success.</p>
     *
     * @param publicId the public UUID of the interaction to update
     * @param request  the partial update request
     * @param actor    the user performing the update
     * @return the updated interaction
     * @throws InteractionNotFoundException if not found
     */
    Interaction update(String publicId, InteractionUpdateRequest request, User actor);

    /**
     * Permanently deletes an interaction and its associated sub-entities
     * ({@code CallLog} and/or {@code EmailLog}), handled by cascade.
     *
     * <p>Publishes an {@code InteractionDeletedEvent} on success.</p>
     *
     * @param publicId the public UUID of the interaction to delete
     * @param actor    the user performing the deletion
     * @throws InteractionNotFoundException if not found
     */
    void delete(String publicId, User actor);
}
