package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.crm.SupportTicket;
import be.steby.CoreProject.dl.enums.crm.SupportTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link SupportTicket} entity operations.
 *
 * <p>Extends both {@link JpaRepository} for standard CRUD operations and
 * {@link JpaSpecificationExecutor} for dynamic multi-criteria filtering
 * used in the ticket list view (filter by status, contact, assignee, etc.).</p>
 *
 * @see be.steby.CoreProject.dal.specifications.crm.SupportTicketSpecification
 */
@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long>,
        JpaSpecificationExecutor<SupportTicket> {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds a ticket by its public UUID.
     *
     * @param publicId the public UUID
     * @return the ticket if found
     */
    Optional<SupportTicket> findByPublicId(String publicId);

    // =========================================================================
    // Contact view
    // =========================================================================

    /**
     * Finds all tickets submitted by a specific contact, ordered by creation date descending.
     *
     * <p>Used to populate the support tickets tab in the contact detail view.</p>
     *
     * @param contactId the internal ID of the contact
     * @return all tickets for that contact, newest first
     */
    List<SupportTicket> findBySubmittedByIdOrderByCreatedAtDesc(Long contactId);

    // =========================================================================
    // Team workload
    // =========================================================================

    /**
     * Finds all open tickets not yet assigned to anyone.
     *
     * <p>Used for the unassigned ticket queue.</p>
     *
     * @return unassigned open tickets, oldest first (FIFO)
     */
    List<SupportTicket> findByAssignedToIsNullAndStatusOrderByCreatedAtAsc(SupportTicketStatus status);

    /**
     * Finds all tickets assigned to a specific team member.
     *
     * <p>Used for the "My Tickets" view.</p>
     *
     * @param assignedToId the internal ID of the team member
     * @return tickets assigned to that person, newest first
     */
    List<SupportTicket> findByAssignedToIdOrderByCreatedAtDesc(Long assignedToId);

    // =========================================================================
    // Reporting
    // =========================================================================

    /**
     * Counts tickets grouped by status.
     *
     * <p>Used for the support dashboard summary cards.</p>
     *
     * @return list of Object[] pairs: [status (SupportTicketStatus), count (Long)]
     */
    @Query("SELECT t.status, COUNT(t) FROM SupportTicket t GROUP BY t.status")
    List<Object[]> countByStatus();

    /**
     * Counts open tickets assigned to a specific team member.
     *
     * @param assignedToId the internal ID of the team member
     * @return number of open tickets for that person
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t " +
           "WHERE t.assignedTo.id = :assignedToId AND t.status != 'CLOSED'")
    long countOpenByAssignedTo(@Param("assignedToId") Long assignedToId);

    // =========================================================================
    // Dashboard stats
    // =========================================================================

    long countByStatus(SupportTicketStatus status);
}
