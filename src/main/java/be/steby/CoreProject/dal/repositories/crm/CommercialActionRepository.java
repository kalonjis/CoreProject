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
     * Finds all tasks linked to a specific contact, ordered by due date ascending.
     *
     * <p>Used to populate the tasks tab in the contact detail view.</p>
     *
     * @param contactId the internal ID of the contact
     * @return all tasks for that contact, soonest due first
     */
    List<CommercialAction> findByContactIdOrderByDueDateAsc(Long contactId);

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

    /**
     * Counts pending tasks assigned to a specific commercial.
     *
     * <p>Used to display the task count badge in the commercial's navigation.</p>
     *
     * @param assignedToId the internal ID of the commercial
     * @return number of pending tasks
     */
    long countByAssignedToIdAndStatus(Long assignedToId, CommercialActionStatus status);
}