package be.steby.CoreProject.pl.domains.pipeline.models.responses;

import be.steby.CoreProject.dl.entities.crm.PipelineStep;

/**
 * Response model for a single {@link PipelineStep}.
 *
 * @param publicId        public UUID of the step
 * @param name            display name of the step
 * @param color           hex color code for the Kanban column, or {@code null} if not set
 * @param position        zero-based display order within the pipeline
 * @param isWon           {@code true} if this is the terminal Won step
 * @param isLost          {@code true} if this is the terminal Lost step
 * @param winProbability  estimated win probability (0–100) for weighted revenue forecasting
 */
public record PipelineStepResponse(
        String publicId,
        String name,
        String color,
        int position,
        boolean isWon,
        boolean isLost,
        int winProbability
) {

    public static PipelineStepResponse fromEntity(PipelineStep step) {
        return new PipelineStepResponse(
                step.getPublicId(),
                step.getName(),
                step.getColor(),
                step.getPosition(),
                step.isWon(),
                step.isLost(),
                step.getWinProbability()
        );
    }
}
