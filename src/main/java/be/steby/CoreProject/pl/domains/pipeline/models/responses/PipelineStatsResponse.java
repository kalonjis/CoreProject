package be.steby.CoreProject.pl.domains.pipeline.models.responses;

import be.steby.CoreProject.bll.domains.pipeline.models.PipelineStatsResult;

import java.util.List;

/**
 * REST response for pipeline conversion and velocity statistics.
 *
 * @param pipelinePublicId  public UUID of the pipeline
 * @param pipelineName      display name of the pipeline
 * @param stages            per-stage stats ordered by position
 * @param winRate           percentage of closed deals won (0–100), null if no closed deals
 * @param avgDealCycleDays  average days from creation to close for WON deals, null if none
 */
public record PipelineStatsResponse(
        String pipelinePublicId,
        String pipelineName,
        List<StageStatsEntry> stages,
        Double winRate,
        Double avgDealCycleDays
) {

    /**
     * @param stagePublicId   public UUID of the stage
     * @param stageName       display name of the stage
     * @param position        zero-based ordering position
     * @param isWon           true if this is the Won terminal stage
     * @param isLost          true if this is the Lost terminal stage
     * @param dealsCurrently  open deals currently at this stage
     * @param dealsEntered    total distinct deals that ever entered this stage
     * @param conversionRate  percentage advancing to the next stage; null for the last stage
     * @param avgDaysInStage  average days spent here by deals that left; null if no history yet
     */
    public record StageStatsEntry(
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

    public static PipelineStatsResponse from(PipelineStatsResult result) {
        List<StageStatsEntry> entries = result.stages().stream()
                .map(s -> new StageStatsEntry(
                        s.stagePublicId(),
                        s.stageName(),
                        s.position(),
                        s.isWon(),
                        s.isLost(),
                        s.dealsCurrently(),
                        s.dealsEntered(),
                        s.conversionRate(),
                        s.avgDaysInStage()
                ))
                .toList();
        return new PipelineStatsResponse(
                result.pipelinePublicId(),
                result.pipelineName(),
                entries,
                result.winRate(),
                result.avgDealCycleDays()
        );
    }
}
