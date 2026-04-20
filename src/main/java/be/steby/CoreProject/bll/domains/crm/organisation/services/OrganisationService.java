package be.steby.CoreProject.bll.domains.crm.organisation.services;

import be.steby.CoreProject.bll.domains.crm.organisation.exceptions.OrganisationMergeException;
import be.steby.CoreProject.bll.domains.crm.organisation.exceptions.OrganisationNameAlreadyExistsException;
import be.steby.CoreProject.bll.domains.crm.organisation.exceptions.OrganisationNotFoundException;
import be.steby.CoreProject.bll.domains.crm.organisation.exceptions.OrganisationValidationException;
import be.steby.CoreProject.bll.domains.crm.organisation.models.OrganisationCreateRequest;
import be.steby.CoreProject.bll.domains.crm.organisation.models.OrganisationFilterRequest;
import be.steby.CoreProject.bll.domains.crm.organisation.models.OrganisationMergeRequest;
import be.steby.CoreProject.bll.domains.crm.organisation.models.OrganisationUpdateRequest;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Organisation;
import be.steby.CoreProject.dl.enums.crm.OrganisationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service for managing {@link Organisation} entities throughout their CRM lifecycle.
 *
 * <h3>Deduplication</h3>
 * <p>Organisation names are enforced as unique (case-insensitive). The service
 * checks for duplicates on creation and on name change during update.</p>
 *
 * <h3>Merge</h3>
 * <p>When two organisations are found to be duplicates, {@link #merge} reassigns
 * all linked contacts to the surviving target and removes the source.</p>
 */
public interface OrganisationService {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds an organisation by its internal database ID.
     *
     * <p><strong>Internal use only</strong> — intended for cross-domain service
     * calls and listeners where the internal ID is already known via JPA
     * relationships. Never expose this ID via API.</p>
     *
     * @param id the internal database ID
     * @return the matching organisation
     * @throws OrganisationNotFoundException if not found
     */
    Organisation getById(Long id);

    /**
     * Finds an organisation by its public UUID.
     *
     * @param publicId the public UUID of the organisation
     * @return the matching organisation
     * @throws OrganisationNotFoundException if not found
     */
    Organisation getByPublicId(String publicId);

    /**
     * Finds an organisation by its exact name (case-insensitive).
     *
     * <p>Used for deduplication checks and autocomplete-based lookups.</p>
     *
     * @param name the organisation name to look up
     * @return the matching organisation
     * @throws OrganisationNotFoundException if not found
     */
    Organisation getByName(String name);

    /**
     * Returns a paginated, filtered list of organisations.
     *
     * <p>All filter fields in {@link OrganisationFilterRequest} are optional.
     * Criteria are combined with AND logic via {@code OrganisationSpecification}.</p>
     *
     * @param filter   the filter criteria (all fields optional)
     * @param pageable pagination and sorting parameters
     * @return a page of matching organisations
     */
    Page<Organisation> findAll(OrganisationFilterRequest filter, Pageable pageable);

    // =========================================================================
    // Creation
    // =========================================================================

    /**
     * Creates a new organisation.
     *
     * <p>Checks that no organisation with the same name (case-insensitive) already
     * exists before persisting. Resolves the optional address by its public UUID.</p>
     *
     * <p>Publishes an {@code OrganisationCreatedEvent} on success.</p>
     *
     * @param request the organisation creation data
     * @param actor   the user performing the operation
     * @return the newly created organisation
     * @throws OrganisationNameAlreadyExistsException
     *         if an organisation with the same name already exists
     * @throws OrganisationValidationException
     *         if a provided field value fails business validation
     */
    Organisation create(OrganisationCreateRequest request, User actor);

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Partially updates an existing organisation's editable fields.
     *
     * <p>Only non-null fields in {@link OrganisationUpdateRequest} are applied.
     * Null fields are ignored and existing values are preserved.</p>
     *
     * <p>If {@code name} is provided and differs from the current name, uniqueness
     * is re-checked. If {@code addressPublicId} is provided, the address is resolved
     * and linked; existing address is replaced.</p>
     *
     * <p>Publishes an {@code OrganisationUpdatedEvent} on success.</p>
     *
     * @param publicId the public UUID of the organisation to update
     * @param request  the partial update request
     * @param actor    the user performing the update
     * @return the updated organisation
     * @throws OrganisationNotFoundException if not found
     * @throws OrganisationNameAlreadyExistsException
     *         if the new name conflicts with an existing organisation
     * @throws OrganisationValidationException
     *         if a provided field value fails business validation
     */
    Organisation update(String publicId, OrganisationUpdateRequest request, User actor);

    /**
     * Manually sets the CRM lifecycle status of an organisation.
     *
     * <p>Both transitions are allowed (PROSPECT → CLIENT and CLIENT → PROSPECT).
     * Publishes an {@code OrganisationUpdatedEvent} on success.</p>
     *
     * @param publicId  the public UUID of the organisation
     * @param newStatus the target status
     * @param actor     the user performing the change
     * @return the updated organisation
     * @throws OrganisationNotFoundException if not found
     */
    Organisation updateStatus(String publicId, OrganisationStatus newStatus, User actor);

    // =========================================================================
    // Merge
    // =========================================================================

    /**
     * Merges two duplicate organisations into one surviving record.
     *
     * <p>The {@code targetPublicId} organisation is kept. All contacts linked to
     * the {@code sourcePublicId} organisation are reassigned to the target.
     * Non-null fields from source are copied to target where the target is blank.</p>
     *
     * <p>Fields copied from source when target is blank:
     * {@code website}, {@code industry}, {@code size}, {@code phone},
     * {@code address}, {@code notes}.</p>
     *
     * <p>Publishes an {@code OrganisationMergedEvent} on success.</p>
     *
     * @param request the merge request (sourcePublicId and targetPublicId must differ)
     * @param actor   the user performing the operation
     * @return the surviving target organisation after the merge
     * @throws OrganisationNotFoundException
     *         if either organisation is not found
     * @throws OrganisationMergeException
     *         if source and target are the same organisation
     */
    Organisation merge(OrganisationMergeRequest request, User actor);
}
