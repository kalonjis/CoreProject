package be.steby.CoreProject.pl.domains.pipeline.models.requests;

import be.steby.CoreProject.bll.domains.pipeline.models.PipelineStepUpdateRequest;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PL request model for partially updating a pipeline step.
 *
 * <p>All fields are optional — only non-null values are applied.
 * Pass an empty string for {@code color} to clear it.</p>
 *
 * @param name   new display name (optional)
 * @param color  new hex color code (optional, pass empty string to clear)
 */
public record UpdatePipelineStepRequest(

        @Size(max = 100, message = "Step name must not exceed 100 characters")
        String name,

        @Pattern(regexp = "^(#[0-9A-Fa-f]{6})?$", message = "Color must be a valid hex code (e.g. #27ae60) or empty to clear")
        String color

) {

    public PipelineStepUpdateRequest toBllModel() {
        return new PipelineStepUpdateRequest(
                name != null ? name.trim() : null,
                color
        );
    }
}
