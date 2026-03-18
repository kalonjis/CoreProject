package be.steby.CoreProject.bll.domains.interaction.services;

import be.steby.CoreProject.bll.domains.interaction.exceptions.InteractionNotFoundException;
import be.steby.CoreProject.bll.domains.interaction.exceptions.InteractionValidationException;
import be.steby.CoreProject.bll.domains.interaction.models.InteractionCreateRequest;
import be.steby.CoreProject.bll.domains.interaction.models.InteractionUpdateRequest;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Interaction;

import java.util.List;

/**
 * Service for managing CRM interactions (timeline touchpoints) across deals and contacts.
 *
 * <h3>Interaction model</h3>
 * <p>An {@link Interaction} records a single touchpoint in the CRM timeline — a call,
 * email, meeting, note, or visit. Every interaction must be linked to at least one
 * deal or contact, and always carries the commercial who performed it.</p>
 *
 * <h3>Type-specific sub-entities</h3>
 * <p>For structured data, {@code CALL} interactions carry a {@code CallLog} and
 * {@code EMAIL} interactions carry an {@code EmailLog}. These are created
 * automatically by {@link #create(InteractionCreateRequest, User)} when the
 * appropriate detail record is present in the request.</p>
 *
 * <h3>Timeline queries</h3>
 * <p>Timelines are always fetched in reverse chronological order (most recent first)
 * via {@link #getTimelineByDeal(String)} and {@link #getTimelineByContact(String)}.</p>
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

    /**
     * Returns the full interaction timeline for a deal, most recent first.
     *
     * @param dealPublicId the public UUID of the deal
     * @return list of interactions linked to that deal (may be empty)
     */
    List<Interaction> getTimelineByDeal(String dealPublicId);

    /**
     * Returns the full interaction timeline for a contact, most recent first.
     *
     * <p>Includes interactions across all deals involving that contact.</p>
     *
     * @param contactPublicId the public UUID of the contact
     * @return list of interactions linked to that contact (may be empty)
     */
    List<Interaction> getTimelineByContact(String contactPublicId);

    /**
     * Returns the full interaction timeline for a lead, most recent first.
     *
     * <p>Used during lead qualification — before conversion to a Contact.</p>
     *
     * @param leadPublicId the public UUID of the lead
     * @return list of interactions linked to that lead (may be empty)
     */
    List<Interaction> getTimelineByLead(String leadPublicId);

    // =========================================================================
    // Write
    // =========================================================================

    /**
     * Logs a new interaction, creating the appropriate type-specific sub-entity
     * ({@code CallLog} or {@code EmailLog}) when applicable.
     *
     * <p>Business rules enforced:</p>
     * <ul>
     *   <li>At least one of {@code dealPublicId} or {@code contactPublicId} must be set</li>
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
