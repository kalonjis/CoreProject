package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.crm.Deal;
import be.steby.CoreProject.dl.enums.crm.DealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Deal} entity operations.
 *
 * <p>Extends both {@link JpaRepository} for standard CRUD operations and
 * {@link JpaSpecificationExecutor} for dynamic multi-criteria filtering
 * used in the Kanban board and deal list views (filter by pipeline, pipelineStep,
 * status, assigned commercial, amount range, expected close date, etc.).</p>
 *
 * <h3>Query strategy</h3>
 * <ul>
 *   <li>Simple lookups → derived query methods ({@code findBy...})</li>
 *   <li>Aggregates and forecasting → {@code @Query} (SUM, COUNT across joins)</li>
 *   <li>Dynamic admin filters → {@code DealSpecification}
 *       via {@code findAll(Specification, Pageable)}</li>
 * </ul>
 *
 * @see be.steby.CoreProject.dal.specifications.crm.DealSpecification
 */
@Repository
public interface DealRepository extends JpaRepository<Deal, Long>,
        JpaSpecificationExecutor<Deal> {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds a deal by its public UUID.
     *
     * @param publicId the public UUID
     * @return the deal if found
     */
    Optional<Deal> findByPublicId(String publicId);

    // =========================================================================
    // Kanban board
    // =========================================================================

    /**
     * Finds all deals currently at a given pipelineStep, ordered by creation date descending.
     *
     * <p>Used to populate a single Kanban column.</p>
     *
     * @param pipelineStepId the internal ID of the pipelineStep
     * @return deals at that pipelineStep, newest first
     */
    List<Deal> findByPipelineStepIdOrderByCreatedAtDesc(Long pipelineStepId);

    /**
     * Finds all open deals in a given pipeline, ordered by creation date descending.
     *
     * <p>Used to load the full Kanban board for a pipeline.</p>
     *
     * @param pipelineId the internal ID of the pipeline
     * @return open deals in that pipeline
     */
    List<Deal> findByPipelineIdAndStatus(Long pipelineId, DealStatus status);

    // =========================================================================
    // Commercial workload
    // =========================================================================

    /**
     * Finds all open deals assigned to a specific commercial.
     *
     * <p>Used for the "My Deals" dashboard view.</p>
     *
     * @param assignedToId the internal ID of the commercial
     * @return open deals assigned to that commercial
     */
    List<Deal> findByAssignedToIdAndStatus(Long assignedToId, DealStatus status);

    // =========================================================================
    // Contact & organisation
    // =========================================================================

    /**
     * Finds all deals involving a specific contact (in any role).
     *
     * <p>Joins through {@code DealContactRole} — a contact may be the primary
     * contact, signer, or any other role on a deal.</p>
     *
     * <p>Used to populate the deals tab in the contact detail view.</p>
     *
     * @param contactId the internal ID of the contact
     * @return all deals for that contact (any role), newest first
     */
    @Query("SELECT d FROM Deal d JOIN d.contactRoles cr WHERE cr.contact.id = :contactId ORDER BY d.createdAt DESC")
    List<Deal> findByContactIdOrderByCreatedAtDesc(@Param("contactId") Long contactId);

    /**
     * Finds all deals linked to a specific organisation.
     *
     * <p>Used to populate the deals tab in the organisation detail view.</p>
     *
     * @param organisationId the internal ID of the organisation
     * @return all deals for that organisation, newest first
     */
    List<Deal> findByOrganisationIdOrderByCreatedAtDesc(Long organisationId);

    // =========================================================================
    // Overdue detection
    // =========================================================================

    /**
     * Finds all open deals whose expected close date is before the given date.
     *
     * <p>Used by the scheduled job that flags overdue deals and creates
     * follow-up tasks automatically.</p>
     *
     * @param today     the reference date (typically {@code LocalDate.now()})
     * @param status    the status to filter by (typically {@code OPEN})
     * @return overdue open deals
     */
    List<Deal> findByExpectedCloseDateBeforeAndStatus(LocalDate today, DealStatus status);

    List<Deal> findByExpectedCloseDateBetweenAndStatusOrderByExpectedCloseDateAsc(
            LocalDate from, LocalDate to, DealStatus status);

    // =========================================================================
    // Reporting & forecasting
    // =========================================================================

    /**
     * Computes the total value of all deals with the given status.
     *
     * <p>Used for revenue reporting (total won) and forecasting (total open).</p>
     *
     * @param status the deal status to aggregate
     * @return the sum of all deal amounts, or {@code null} if no deals match
     */
    @Query("SELECT SUM(d.amount) FROM Deal d WHERE d.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") DealStatus status);

    /**
     * Computes the total value of all open deals assigned to a specific commercial.
     *
     * <p>Used for individual revenue forecasting in the team dashboard.</p>
     *
     * @param assignedToId the internal ID of the commercial
     * @return the sum of open deal amounts for that commercial
     */
    @Query("SELECT SUM(d.amount) FROM Deal d " +
           "WHERE d.assignedTo.id = :assignedToId AND d.status = 'OPEN'")
    BigDecimal sumOpenAmountByAssignedTo(@Param("assignedToId") Long assignedToId);

    /**
     * Counts deals grouped by pipelineStep for a given pipeline.
     *
     * <p>Used to display the deal count badge on each Kanban column header.</p>
     *
     * @param pipelineId the internal ID of the pipeline
     * @return list of Object[] pairs: [pipelineStepId (Long), count (Long)]
     */
    @Query("SELECT d.pipelineStep.id, COUNT(d) FROM Deal d " +
           "WHERE d.pipeline.id = :pipelineId AND d.status = 'OPEN' " +
           "GROUP BY d.pipelineStep.id")
    List<Object[]> countOpenDealsByStage(@Param("pipelineId") Long pipelineId);

    // =========================================================================
    // Dashboard stats
    // =========================================================================

    long countByStatus(DealStatus status);

    @Query("SELECT COUNT(d) FROM Deal d WHERE d.status = 'WON' AND d.closedAt >= :since")
    long countWonSince(@Param("since") java.time.Instant since);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM Deal d WHERE d.status = 'WON' AND d.closedAt >= :since")
    java.math.BigDecimal sumAmountWonSince(@Param("since") java.time.Instant since);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM Deal d WHERE d.status = 'WON' AND d.closedAt >= :from AND d.closedAt < :to")
    java.math.BigDecimal sumAmountWonBetween(@Param("from") java.time.Instant from, @Param("to") java.time.Instant to);

    @Query("SELECT COUNT(d) FROM Deal d WHERE d.status = 'WON' AND d.closedAt >= :from AND d.closedAt < :to")
    long countWonBetween(@Param("from") java.time.Instant from, @Param("to") java.time.Instant to);

    /**
     * Computes the weighted revenue forecast for all open deals.
     *
     * <p>Formula: {@code SUM(deal.amount × stage.winProbability / 100)}
     * for all OPEN deals with a non-null amount.</p>
     *
     * <p>Used on the dashboard to show the expected revenue given the
     * current pipeline composition and per-stage win probabilities.</p>
     *
     * @return weighted forecast revenue, or {@code 0} if no qualifying deals exist
     */
    @Query("SELECT COALESCE(SUM(d.amount * d.pipelineStep.winProbability / 100.0), 0) " +
           "FROM Deal d WHERE d.status = 'OPEN' AND d.amount IS NOT NULL")
    java.math.BigDecimal sumWeightedForecast();

    @Query("SELECT d FROM Deal d WHERE LOWER(d.title) LIKE :kw")
    List<Deal> searchByKeyword(@Param("kw") String keyword, org.springframework.data.domain.Pageable pageable);

    // =========================================================================
    // Pipeline stats
    // =========================================================================

    /**
     * Counts deals in a pipeline by status.
     *
     * <p>Used to compute win/loss counts for overall pipeline conversion rate.</p>
     *
     * @param pipelineId the internal ID of the pipeline
     * @param status     the deal status to filter by
     * @return number of deals in that pipeline with the given status
     */
    long countByPipelineIdAndStatus(Long pipelineId, DealStatus status);

    /**
     * Computes the average deal cycle in days for won deals in a pipeline.
     *
     * <p>Cycle = {@code closedAt - createdAt}. Only WON deals are included.</p>
     *
     * @param pipelineId the internal ID of the pipeline
     * @return average days from creation to close, or {@code null} if no WON deals exist
     */
    @Query(value = """
            SELECT AVG(EXTRACT(EPOCH FROM (closed_at - created_at)) / 86400.0)
            FROM crm_deal
            WHERE pipeline_id = :pipelineId
              AND status = 'WON'
              AND closed_at IS NOT NULL
            """, nativeQuery = true)
    Double avgDealCycleDaysForPipeline(@Param("pipelineId") Long pipelineId);
}