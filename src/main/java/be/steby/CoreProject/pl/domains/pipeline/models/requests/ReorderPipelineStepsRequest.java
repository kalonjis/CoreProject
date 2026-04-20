package be.steby.CoreProject.pl.domains.pipeline.models.requests;

import be.steby.CoreProject.bll.domains.crm.pipeline.models.PipelineStepReorderRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * PL request model for atomically reordering all steps of a pipeline.
 *
 * <p>The full ordered list of steps must be provided — partial reorders are not supported.</p>
 *
 * @param orderedSteps the complete list of steps with their new positions (required, not empty)
 */
public record ReorderPipelineStepsRequest(

        @NotEmpty(message = "Ordered steps list must not be empty")
        @Valid
        List<StepPosition> orderedSteps

) {

    /**
     * Maps a single step to its new position.
     *
     * @param stepPublicId public UUID of the step (required)
     * @param newPosition  new zero-based position within the pipeline (required)
     */
    public record StepPosition(

            @NotNull(message = "Step public ID is required")
            String stepPublicId,

            int newPosition

    ) {}

    public PipelineStepReorderRequest toBllModel() {
        List<PipelineStepReorderRequest.StepPosition> mapped = orderedSteps.stream()
                .map(s -> new PipelineStepReorderRequest.StepPosition(s.stepPublicId(), s.newPosition()))
                .toList();
        return new PipelineStepReorderRequest(mapped);
    }
}
