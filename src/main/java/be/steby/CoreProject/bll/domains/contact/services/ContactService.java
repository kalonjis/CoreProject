package be.steby.CoreProject.bll.domains.contact.services;

import be.steby.CoreProject.bll.domains.contact.models.*;
import be.steby.CoreProject.bll.domains.contact.exceptions.*;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.dl.enums.crm.ContactStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service for managing contacts throughout their full CRM lifecycle.
 *
 * <h3>Creation flows</h3>
 * <pre>
 * Flow 1 — Automatic (lead form):
 *   LeadSubmittedEvent → ContactCreationListener → {@link #createFromLead(Lead)}
 *
 * Flow 2 — Manual (commercial encodes directly):
 *   {@link #create(ContactCreateRequest, User)}
 * </pre>
 *
 * <p>In both cases, the contact is created immediately with available data.
 * The commercial then enriches the contact via {@link #update} directly on
 * the contact record — no conversion step creates data.</p>
 *
 * <h3>CRM lifecycle</h3>
 * <pre>
 * NEW ──► ENGAGED ──► QUALIFIED ──► CLIENT
 *                                      │
 *              LOST ◄──────────────────┘
 *              INACTIVE (dormant)
 * </pre>
 */
public interface ContactService {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds a contact by its internal database ID.
     *
     * <p><strong>Internal use only</strong> — intended for cross-domain service
     * calls and listeners where the internal ID is already known via JPA
     * relationships. Never expose this ID via API.</p>
     *
     * @param id the internal database ID
     * @return the matching contact
     * @throws ContactNotFoundException if not found
     */
    Contact getById(Long id);

    /**
     * Finds a contact by its public UUID.
     *
     * @param publicId the public UUID of the contact
     * @return the matching contact
     * @throws ContactNotFoundException if not found
     */
    Contact getByPublicId(String publicId);

    /**
     * Finds a contact by their email address (case-insensitive).
     *
     * @param email the email address to look up
     * @return the matching contact
     * @throws ContactNotFoundException if not found
     */
    Contact getByEmail(String email);

    /**
     * Returns a paginated, filtered list of contacts.
     *
     * <p>All filter fields in {@link ContactFilterRequest} are optional.
     * Criteria are combined with AND logic via {@code ContactSpecification}.</p>
     *
     * @param filter   the filter criteria (all fields optional)
     * @param pageable pagination and sorting parameters
     * @return a page of matching contacts
     */
    Page<Contact> findAll(ContactFilterRequest filter, Pageable pageable);

    /**
     * Returns all contacts belonging to a given organisation.
     *
     * @param organisationPublicId the public UUID of the organisation
     * @return list of contacts for that organisation (may be empty)
     */
    List<Contact> findByOrganisation(String organisationPublicId);

    // =========================================================================
    // Creation
    // =========================================================================

    /**
     * Creates a contact automatically from a submitted lead.
     *
     * <p>Called by {@code ContactCreationListener} upon receiving a
     * {@code LeadSubmittedEvent}. The contact is created immediately
     * with available lead data: {@code email}, {@code name}, {@code phone}.
     * Status defaults to {@code NEW}. The lead is linked via {@code originLead}.</p>
     *
     * <p>If a contact with the same email already exists, no new contact
     * is created — the existing one is returned and linked to the lead.</p>
     *
     * <p>Publishes a {@code ContactCreatedEvent} on success.</p>
     *
     * @param lead the submitted lead to create a contact from
     * @return the newly created (or existing) contact
     */
    Contact createFromLead(Lead lead);

    /**
     * Creates a contact manually, without going through the lead flow.
     *
     * <p>Used when a commercial encodes a contact directly — e.g., a phone
     * call, a trade show encounter, or a business card exchange.</p>
     *
     * <p>Status defaults to {@code NEW}. Assignment is a separate operation.</p>
     *
     * <p>Publishes a {@code ContactCreatedEvent} on success.</p>
     *
     * @param request the contact creation data
     * @param actor   the commercial performing the operation
     * @return the newly created contact
     * @throws ContactEmailAlreadyExistsException
     *         if a contact with the same email already exists
     * @throws ContactValidationException
     *         if a provided field value fails business validation
     */
    Contact create(ContactCreateRequest request, User actor);

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Partially updates an existing contact's editable fields.
     *
     * <p>Only non-null fields in {@link ContactUpdateRequest} are applied.
     * Null fields are ignored and existing values are preserved.</p>
     *
     * <p>Fields managed by dedicated operations, excluded here:</p>
     * <ul>
     *   <li>{@code status} — use {@link #updateStatus}</li>
     *   <li>{@code organisation} — use {@link #linkOrganisation}</li>
     *   <li>{@code linkedUser} — use {@link #linkUser}</li>
     *   <li>{@code originLead} — immutable after creation</li>
     * </ul>
     *
     * <p>Publishes a {@code ContactUpdatedEvent} on success.</p>
     *
     * @param publicId the public UUID of the contact to update
     * @param request  the partial update request
     * @param actor    the user performing the update
     * @return the updated contact
     * @throws ContactNotFoundException if not found
     * @throws ContactEmailAlreadyExistsException
     *         if the new email conflicts with another existing contact
     * @throws ContactValidationException
     *         if a provided field value fails business validation
     */
    Contact update(String publicId, ContactUpdateRequest request, User actor);

    /**
     * Transitions a contact to a new CRM lifecycle status.
     *
     * <p>Validates that the transition is allowed from the current status.
     * Publishes a {@code ContactStatusChangedEvent} on success.</p>
     *
     * @param publicId  the public UUID of the contact
     * @param newStatus the target status
     * @param actor     the user performing the transition
     * @return the updated contact
     * @throws ContactNotFoundException if not found
     * @throws ContactStatusTransitionException
     *         if the transition from the current status to {@code newStatus} is not allowed
     */
    Contact updateStatus(String publicId, ContactStatus newStatus, User actor);

    // =========================================================================
    // Relations
    // =========================================================================

    /**
     * Links a platform account ({@link User}) to a contact.
     *
     * <p>Established when the contact signs a contract and creates a platform
     * account. Once set, it cannot be overwritten.</p>
     *
     * @param contactPublicId the public UUID of the contact
     * @param user            the platform account to link
     * @return the updated contact
     * @throws ContactNotFoundException if not found
     * @throws ContactAlreadyLinkedToUserException
     *         if the contact already has a linked user, or if the user is already linked to another contact
     */
    Contact linkUser(String contactPublicId, User user);

    /**
     * Links or changes the organisation associated with a contact.
     *
     * <p>Passing {@code null} as {@code organisationPublicId} removes the
     * current organisation link, making the contact independent.</p>
     *
     * @param contactPublicId      the public UUID of the contact
     * @param organisationPublicId the public UUID of the organisation to link, or {@code null} to unlink
     * @param actor                the user performing the operation
     * @return the updated contact
     * @throws ContactNotFoundException if not found
     */
    Contact linkOrganisation(String contactPublicId, String organisationPublicId, User actor);

    /**
     * Assigns or unassigns a commercial to a contact.
     *
     * <p>Passing {@code null} as {@code request.commercialPublicId()} removes the
     * current assignee. Publishes a {@code ContactAssignedEvent} on success.</p>
     *
     * @param contactPublicId the public UUID of the contact
     * @param request         the assignment request (commercialPublicId may be null to unassign)
     * @param actor           the user performing the operation
     * @return the updated contact
     * @throws ContactNotFoundException if not found
     */
    Contact assign(String contactPublicId, ContactAssignRequest request, User actor);

    /**
     * Merges two duplicate contacts into one surviving record.
     *
     * <p>The {@code targetPublicId} contact is kept. The {@code sourcePublicId}
     * contact is archived (status set to {@code INACTIVE}) after non-null fields
     * are copied from source to target where the target is blank.</p>
     *
     * <p>Fields copied from source when target is blank:
     * {@code phone}, {@code jobTitle}, {@code notes}, {@code organisation},
     * {@code originLead}.</p>
     *
     * <p>Publishes a {@code ContactMergedEvent} on success.</p>
     *
     * @param request the merge request (sourcePublicId and targetPublicId must differ)
     * @param actor   the user performing the operation
     * @return the surviving target contact after the merge
     * @throws ContactNotFoundException if either contact is not found
     * @throws ContactValidationException
     *         if source and target are the same contact
     */
    Contact merge(ContactMergeRequest request, User actor);
}