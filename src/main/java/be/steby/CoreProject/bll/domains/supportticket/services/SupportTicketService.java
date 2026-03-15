package be.steby.CoreProject.bll.domains.supportticket.services;

import be.steby.CoreProject.bll.domains.supportticket.exceptions.SupportTicketAlreadyClosedException;
import be.steby.CoreProject.bll.domains.supportticket.exceptions.SupportTicketNotFoundException;
import be.steby.CoreProject.bll.domains.supportticket.exceptions.SupportTicketStatusTransitionException;
import be.steby.CoreProject.bll.domains.supportticket.models.SupportTicketAssignRequest;
import be.steby.CoreProject.bll.domains.supportticket.models.SupportTicketChangeStatusRequest;
import be.steby.CoreProject.bll.domains.supportticket.models.SupportTicketCreateRequest;
import be.steby.CoreProject.bll.domains.supportticket.models.SupportTicketFilterRequest;
import be.steby.CoreProject.bll.domains.supportticket.models.SupportTicketUpdateRequest;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service for managing support tickets throughout their lifecycle.
 *
 * <h3>Ticket lifecycle</h3>
 * <pre>
 * OPEN ──► IN_PROGRESS ──► RESOLVED ──► CLOSED
 *  └──────────────────────────────────► CLOSED (direct close)
 * </pre>
 *
 * <h3>Closed ticket guard</h3>
 * <p>Update, status-change, and assign operations are blocked on closed tickets.</p>
 */
public interface SupportTicketService {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds a ticket by its internal database ID.
     *
     * <p><strong>Internal use only.</strong></p>
     *
     * @param id the internal database ID
     * @return the matching ticket
     * @throws SupportTicketNotFoundException if not found
     */
    SupportTicket getById(Long id);

    /**
     * Finds a ticket by its public UUID.
     *
     * @param publicId the public UUID of the ticket
     * @return the matching ticket
     * @throws SupportTicketNotFoundException if not found
     */
    SupportTicket getByPublicId(String publicId);

    /**
     * Returns a paginated, filtered list of tickets.
     *
     * @param filter   the filter criteria (all fields optional)
     * @param pageable pagination and sorting parameters
     * @return a page of matching tickets
     */
    Page<SupportTicket> findAll(SupportTicketFilterRequest filter, Pageable pageable);

    /**
     * Returns all tickets submitted by a given contact.
     *
     * @param contactPublicId the public UUID of the contact
     * @return list of tickets for that contact (may be empty)
     */
    List<SupportTicket> findByContact(String contactPublicId);

    // =========================================================================
    // Creation & update
    // =========================================================================

    /**
     * Creates a new support ticket on behalf of a contact.
     *
     * <p>Publishes a {@code SupportTicketCreatedEvent} on success.</p>
     *
     * @param request the ticket creation data
     * @param actor   the user performing the operation
     * @return the newly created ticket
     */
    SupportTicket create(SupportTicketCreateRequest request, User actor);

    /**
     * Partially updates an existing ticket's content fields.
     *
     * <p>Only non-null fields in {@link SupportTicketUpdateRequest} are applied.</p>
     *
     * <p>Publishes a {@code SupportTicketUpdatedEvent} on success.</p>
     *
     * @param publicId the public UUID of the ticket to update
     * @param request  the partial update request
     * @param actor    the user performing the update
     * @return the updated ticket
     * @throws SupportTicketNotFoundException      if not found
     * @throws SupportTicketAlreadyClosedException if the ticket is already closed
     */
    SupportTicket update(String publicId, SupportTicketUpdateRequest request, User actor);

    // =========================================================================
    // Status
    // =========================================================================

    /**
     * Changes a ticket's lifecycle status.
     *
     * <p>Validates allowed transitions. Closing a ticket publishes an additional
     * {@code SupportTicketClosedEvent}.</p>
     *
     * <p>Always publishes a {@code SupportTicketStatusChangedEvent}.</p>
     *
     * @param publicId the public UUID of the ticket
     * @param request  the status change request
     * @param actor    the user performing the action
     * @return the updated ticket
     * @throws SupportTicketNotFoundException        if not found
     * @throws SupportTicketAlreadyClosedException   if the ticket is already closed
     * @throws SupportTicketStatusTransitionException if the transition is not allowed
     */
    SupportTicket changeStatus(String publicId, SupportTicketChangeStatusRequest request, User actor);

    // =========================================================================
    // Assignment
    // =========================================================================

    /**
     * Assigns or unassigns a ticket to a team member.
     *
     * <p>Passing {@code null} as {@code request.assignedToPublicId()} removes the
     * current assignee. Publishes a {@code SupportTicketAssignedEvent} on success.</p>
     *
     * @param publicId the public UUID of the ticket
     * @param request  the assignment request (assignedToPublicId may be null to unassign)
     * @param actor    the user performing the operation
     * @return the updated ticket
     * @throws SupportTicketNotFoundException      if not found
     * @throws SupportTicketAlreadyClosedException if the ticket is already closed
     */
    SupportTicket assign(String publicId, SupportTicketAssignRequest request, User actor);
}
