package be.steby.CoreProject.pl.domains.pipeline.models.requests;

import be.steby.CoreProject.bll.domains.pipeline.models.PipelineStepCreateRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PL request model for adding a new step to a pipeline.
 *
 * @param name     display name of the step (required)
 * @param color    hex color code for the Kanban column (optional, e.g. "#27ae60")
 * @param position zero-based display order within the pipeline (required)
 * @param isWon    marks this step as the terminal Won step
 * @param isLost   marks this step as the terminal Lost step
 */
public record AddPipelineStepRequest(

        @NotBlank(message = "Step name is required")
        @Size(max = 100, message = "Step name must not exceed 100 characters")
        String name,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex code (e.g. #27ae60)")
        String color,

        int position,

        boolean isWon,

        boolean isLost

) {

    public PipelineStepCreateRequest toBllModel() {
        return new PipelineStepCreateRequest(
                name.trim(),
                color,
                position,
                isWon,
                isLost
        );
    }
}
