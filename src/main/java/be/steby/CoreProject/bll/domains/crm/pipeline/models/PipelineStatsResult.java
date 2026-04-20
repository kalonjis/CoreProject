package be.steby.CoreProject.bll.domains.crm.pipeline.models;

import java.util.List;

/**
 * Read model returned by {@code PipelineService#getStats}.
 *
 * <p>Aggregates per-stage funnel metrics and overall pipeline performance
 * computed from {@code DealStageHistory} entries.</p>
 *
 * @param pipelinePublicId  public UUID of the pipeline
 * @param pipelineName      display name of the pipeline
 * @param stages            per-stage stats ordered by position
 * @param winRate           percentage of closed deals that were won (0–100), or null if no closed deals
 * @param avgDealCycleDays  average days from deal creation to close (WON deals only), or null if none
 */
public record PipelineStatsResult(
        String pipelinePublicId,
        String pipelineName,
        List<StageStats> stages,
        Double winRate,
        Double avgDealCycleDays
) {

    /**
     * Per-stage funnel metrics.
     *
     * @param stagePublicId    public UUID of the stage
     * @param stageName        display name of the stage
     * @param position         zero-based ordering position within the pipeline
     * @param isWon            whether this is the Won terminal stage
     * @param isLost           whether this is the Lost terminal stage
     * @param dealsCurrently   deals currently open and sitting at this stage
     * @param dealsEntered     total distinct deals that ever entered this stage
     * @param conversionRate   percentage of deals that moved on to the next stage (null for the last stage)
     * @param avgDaysInStage   average days spent in this stage for deals that already left it, or null if none
     */
    public record StageStats(
            String stagePublicId,
            String stageName,
            int position,
            boolean isWon,
            boolean isLost,
            long dealsCurrently,
            long dealsEntered,
            Double conversionRate,
            Double avgDaysInStage
    ) {}
}
