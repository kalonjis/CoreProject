package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.crm.DealStageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link DealStageHistory} operations.
 *
 * <p>This is an append-only log — entries are inserted but never updated
 * except to close them (set {@code exitedAt}) when a deal moves to the next stage.</p>
 */
@Repository
public interface DealStageHistoryRepository extends JpaRepository<DealStageHistory, Long> {

    /**
     * Finds the currently open history entry for a deal (the stage the deal is in now).
     *
     * <p>Used when moving a deal to close the previous entry before opening a new one.</p>
     *
     * @param dealId the internal ID of the deal
     * @return the open entry, if any
     */
    @Query("SELECT h FROM DealStageHistory h WHERE h.deal.id = :dealId AND h.exitedAt IS NULL")
    Optional<DealStageHistory> findOpenEntryByDealId(@Param("dealId") Long dealId);

    /**
     * Counts the distinct deals that ever entered a given pipeline step.
     *
     * <p>Used to compute the funnel volume for each stage: how many deals
     * reached this stage at any point in time.</p>
     *
     * @param stepId the internal ID of the pipeline step
     * @return number of distinct deals that entered this step
     */
    @Query("SELECT COUNT(DISTINCT h.deal.id) FROM DealStageHistory h WHERE h.pipelineStep.id = :stepId")
    long countDealsEnteredStep(@Param("stepId") Long stepId);

    /**
     * Computes the average number of days deals spent in a given pipeline step.
     *
     * <p>Only considers closed entries ({@code exitedAt IS NOT NULL}).
     * Deals still in the stage are excluded to avoid skewing the average upward.</p>
     *
     * @param stepId the internal ID of the pipeline step
     * @return average days in this step, or {@code null} if no closed entries exist
     */
    @Query(value = """
            SELECT AVG(EXTRACT(EPOCH FROM (exited_at - entered_at)) / 86400.0)
            FROM deal_stage_history
            WHERE pipeline_step_id = :stepId
              AND exited_at IS NOT NULL
            """, nativeQuery = true)
    Double avgDaysInStep(@Param("stepId") Long stepId);
}
