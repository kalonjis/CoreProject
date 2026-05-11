package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.crm.CommercialAction;
import be.steby.CoreProject.dl.enums.crm.CommercialActionPriority;
import be.steby.CoreProject.dl.enums.crm.CommercialActionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link CommercialAction} entity operations.
 *
 * <p>Tasks are always accessed in the context of a commercial's workload,
 * a deal, or a contact. Targeted derived queries cover all needed access
 * patterns — no {@code JpaSpecificationExecutor} needed.</p>
 *
 * <h3>Query strategy</h3>
 * <ul>
 *   <li>Workload & context queries → derived query methods</li>
 *   <li>Overdue detection → {@code @Query} with date comparison</li>
 * </ul>
 */
@Repository
public interface CommercialActionRepository extends JpaRepository<CommercialAction, Long> {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds a task by its public UUID.
     *
     * @param publicId the public UUID
     * @return the task if found
     */
    Optional<CommercialAction> findByPublicId(String publicId);

    // =========================================================================
    // Commercial workload — "My Tasks" view
    // =========================================================================

    /**
     * Finds all pending tasks assigned to a specific commercial,
     * ordered by due date ascending (most urgent first).
     *
     * <p>Used to populate the "My Tasks" dashboard for a commercial.</p>
     *
     * @param assignedToId the internal ID of the commercial
     * @return pending tasks for that commercial, soonest due first
     */
    List<CommercialAction> findByAssignedToIdAndStatusOrderByDueDateAsc(Long assignedToId, CommercialActionStatus status);

    /**
     * Finds all pending high-priority tasks assigned to a specific commercial.
     *
     * <p>Used to populate the urgent tasks widget in the commercial dashboard.</p>
     *
     * @param assignedToId the internal ID of the commercial
     * @param priority     the priority level to filter by
     * @param status       the task status to filter by
     * @return matching tasks, soonest due first
     */
    List<CommercialAction> findByAssignedToIdAndPriorityAndStatusOrderByDueDateAsc(
            Long assignedToId,
            CommercialActionPriority priority,
            CommercialActionStatus status
    );

    // =========================================================================
    // Deal & contact context
    // =========================================================================

    /**
     * Finds all tasks linked to a specific deal, ordered by due date ascending.
     *
     * <p>Used to populate the tasks tab in the deal detail view.</p>
     *
     * @param dealId the internal ID of the deal
     * @return all tasks for that deal, soonest due first
     */
    List<CommercialAction> findByDealIdOrderByDueDateAsc(Long dealId);

    /**
     * Finds all tasks linked to a deal OR to the deal's contact, ordered by due date ascending.
     *
     * <p>Used for the deal action list: shows both deal-specific actions and
     * the contact's actions (e.g. tasks created during lead qualification before
     * the deal existed). Null due dates are sorted last.</p>
     *
     * @param dealId    the internal ID of the deal
     * @param contactId the internal ID of the deal's primary contact
     * @return all tasks for that deal or contact, soonest due first
     */
    /**
     * Finds all actions linked to a deal OR to any of the given contacts.
     * Used for the deal action list with multi-contact support.
     */
    @Query("SELECT DISTINCT ca FROM CommercialAction ca LEFT JOIN ca.deal d LEFT JOIN ca.contact c WHERE d.id = :dealId OR c.id IN :contactIds ORDER BY ca.dueDate ASC NULLS LAST")
    List<CommercialAction> findByDealOrContactsOrderByDueDateAsc(@Param("dealId") Long dealId, @Param("contactIds") java.util.Collection<Long> contactIds);

    /**
     * Finds all tasks linked to a specific contact, ordered by due date ascending.
     *
     * <p>Used to populate the tasks tab in the contact detail view.</p>
     *
     * @param contactId the internal ID of the contact
     * @return all tasks for that contact, soonest due first
     */
    List<CommercialAction> findByContactIdOrderByDueDateAsc(Long contactId);

    /**
     * Finds all tasks linked to a contact directly OR via a deal where that contact
     * is the primary contact, ordered by due date ascending.
     *
     * <p>Used to populate the contact action list: shows both contact-level actions
     * and actions created on the contact's deals.</p>
     *
     * @param contactId the internal ID of the contact
     * @return all tasks for that contact or their deals, soonest due first
     */
    @Query("SELECT DISTINCT ca FROM CommercialAction ca LEFT JOIN ca.contact c LEFT JOIN ca.deal d LEFT JOIN d.contactRoles dcr LEFT JOIN dcr.contact dc WHERE c.id = :contactId OR dc.id = :contactId ORDER BY ca.dueDate ASC NULLS LAST")
    List<CommercialAction> findByContactOrContactDealOrderByDueDateAsc(@Param("contactId") Long contactId);

    /**
     * Finds all tasks linked to a specific lead, ordered by due date ascending.
     *
     * <p>Used during lead qualification — before conversion to a Contact.</p>
     *
     * @param leadId the internal ID of the lead
     * @return all tasks for that lead, soonest due first
     */
    List<CommercialAction> findByLeadIdOrderByDueDateAsc(Long leadId);

    // =========================================================================
    // Overdue detection
    // =========================================================================

    /**
     * Finds all pending tasks whose due date has passed.
     *
     * <p>Used by the scheduled job that flags overdue tasks and sends
     * reminder notifications to the assigned commercial.</p>
     *
     * @param now the reference instant (typically {@code Instant.now()})
     * @return overdue pending tasks
     */
    @Query("SELECT c FROM CommercialAction c " +
           "WHERE c.status = 'PENDING' " +
           "AND c.dueDate IS NOT NULL " +
           "AND c.dueDate < :now")
    List<CommercialAction> findAllOverdue(@Param("now") Instant now);

    @Query("SELECT c FROM CommercialAction c " +
           "WHERE c.status = 'PENDING' " +
           "AND c.dueDate IS NOT NULL " +
           "AND c.dueDate >= :startOfDay " +
           "AND c.dueDate < :endOfDay " +
           "ORDER BY c.dueDate ASC")
    List<CommercialAction> findDueToday(@Param("startOfDay") Instant startOfDay,
                                        @Param("endOfDay")   Instant endOfDay);

    @Query("SELECT COUNT(c) FROM CommercialAction c " +
           "WHERE c.status = 'PENDING' " +
           "AND c.dueDate IS NOT NULL " +
           "AND c.dueDate < :now")
    long countAllOverdue(@Param("now") Instant now);

    /**
     * Finds pending actions whose reminder time has arrived and has not yet been sent.
     *
     * <p>Used by {@code CommercialActionReminderScheduler} every 15 minutes.
     * The {@code reminderSentAt IS NULL} guard ensures idempotence.</p>
     *
     * @param now the reference instant (typically {@code Instant.now()})
     * @return actions ready for reminder dispatch
     */
    @Query("SELECT c FROM CommercialAction c " +
           "WHERE c.status = 'PENDING' " +
           "AND c.reminderAt IS NOT NULL " +
           "AND c.reminderAt <= :now " +
           "AND c.reminderSentAt IS NULL")
    List<CommercialAction> findDueReminders(@Param("now") Instant now);

    /**
     * Counts pending tasks assigned to a specific commercial.
     *
     * <p>Used to display the task count badge in the commercial's navigation.</p>
     *
     * @param assignedToId the internal ID of the commercial
     * @return number of pending tasks
     */
    long countByAssignedToIdAndStatus(Long assignedToId, CommercialActionStatus status);

    // =========================================================================
    // Timeline — completed actions (unified timeline)
    // =========================================================================

    /**
     * Finds all completed actions linked to a deal, ordered by completion date descending.
     *
     * <p>Used to populate the unified timeline view alongside {@link Interaction} entries.</p>
     *
     * @param dealId the internal ID of the deal
     * @param status must be {@code DONE}
     * @return completed actions for that deal, most recently completed first
     */
    List<CommercialAction> findByDealIdAndStatusOrderByCompletedAtDesc(Long dealId, CommercialActionStatus status);

    /**
     * Finds all completed actions linked to a deal OR to the deal's contact,
     * ordered by completion date descending.
     *
     * <p>Used for the unified deal timeline: shows both deal-specific completed actions
     * and the contact's pre-deal history (e.g. actions from lead qualification).</p>
     *
     * @param dealId    the internal ID of the deal
     * @param contactId the internal ID of the deal's primary contact
     * @param status    must be {@code DONE}
     * @return merged completed actions, most recently completed first
     */
    /**
     * Finds completed actions linked to a deal OR to any of the given contacts.
     * Used for the unified deal timeline with multi-contact support.
     */
    @Query("SELECT DISTINCT ca FROM CommercialAction ca LEFT JOIN ca.deal d LEFT JOIN ca.contact c WHERE (d.id = :dealId OR c.id IN :contactIds) AND ca.status = :status ORDER BY ca.completedAt DESC")
    List<CommercialAction> findByDealOrContactsAndStatus(@Param("dealId") Long dealId, @Param("contactIds") java.util.Collection<Long> contactIds, @Param("status") CommercialActionStatus status);

    /**
     * Finds all completed actions linked to a contact, ordered by completion date descending.
     *
     * <p>Used to populate the unified timeline view alongside {@link Interaction} entries.</p>
     *
     * @param contactId the internal ID of the contact
     * @param status must be {@code DONE}
     * @return completed actions for that contact, most recently completed first
     */
    List<CommercialAction> findByContactIdAndStatusOrderByCompletedAtDesc(Long contactId, CommercialActionStatus status);

    /**
     * Finds all completed actions linked to a contact directly OR via a deal where that
     * contact is the primary contact, ordered by completion date descending.
     *
     * <p>Used for the unified contact timeline: includes actions completed on the contact's deals.</p>
     *
     * @param contactId the internal ID of the contact
     * @param status must be {@code DONE}
     * @return completed actions for that contact or their deals, most recently completed first
     */
    @Query("SELECT DISTINCT ca FROM CommercialAction ca LEFT JOIN ca.contact c LEFT JOIN ca.deal d LEFT JOIN d.contactRoles dcr LEFT JOIN dcr.contact dc WHERE (c.id = :contactId OR dc.id = :contactId) AND ca.status = :status ORDER BY ca.completedAt DESC")
    List<CommercialAction> findByContactOrContactDealAndStatusOrderByCompletedAtDesc(@Param("contactId") Long contactId, @Param("status") CommercialActionStatus status);

    /**
     * Finds all completed actions linked to a lead, ordered by completion date descending.
     *
     * <p>Used to populate the unified timeline view alongside {@link Interaction} entries.</p>
     *
     * @param leadId the internal ID of the lead
     * @param status must be {@code DONE}
     * @return completed actions for that lead, most recently completed first
     */
    List<CommercialAction> findByLeadIdAndStatusOrderByCompletedAtDesc(Long leadId, CommercialActionStatus status);

    /**
     * Finds all completed actions linked to any of the given contacts, ordered by completion date descending.
     *
     * <p>Used to populate the aggregated organisation timeline — merges completed activity
     * from all contacts belonging to the organisation.</p>
     *
     * @param contactIds the internal IDs of the organisation's contacts
     * @param status     must be {@code DONE}
     * @return completed actions for those contacts, most recently completed first
     */
    List<CommercialAction> findByContactIdInAndStatusOrderByCompletedAtDesc(
            java.util.Collection<Long> contactIds,
            CommercialActionStatus status
    );
}