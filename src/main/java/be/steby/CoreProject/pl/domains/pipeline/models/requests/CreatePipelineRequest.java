package be.steby.CoreProject.pl.domains.pipeline.models.requests;

import be.steby.CoreProject.bll.domains.crm.pipeline.models.PipelineCreateRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PL request model for creating a new pipeline.
 *
 * @param name         display name (required)
 * @param description  optional description of the pipeline's purpose
 * @param isDefault    whether this pipeline should become the default for new deals
 * @param displayOrder position in the UI pipeline selector (lower = first)
 */
public record CreatePipelineRequest(

        @NotBlank(message = "Pipeline name is required")
        @Size(max = 100, message = "Pipeline name must not exceed 100 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        boolean isDefault,

        int displayOrder

) {

    public PipelineCreateRequest toBllModel() {
        return new PipelineCreateRequest(
                name.trim(),
                description != null ? description.trim() : null,
                isDefault,
                displayOrder
        );
    }
}
