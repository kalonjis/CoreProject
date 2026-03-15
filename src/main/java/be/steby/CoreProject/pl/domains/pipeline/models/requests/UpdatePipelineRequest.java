package be.steby.CoreProject.pl.domains.pipeline.models.requests;

import be.steby.CoreProject.bll.domains.pipeline.models.PipelineUpdateRequest;
import jakarta.validation.constraints.Size;

/**
 * PL request model for partially updating a pipeline.
 *
 * <p>All fields are optional — only non-null values are applied.</p>
 *
 * @param name         new display name (optional)
 * @param description  new description (optional, pass empty string to clear)
 * @param isDefault    new default flag (optional)
 * @param displayOrder new display order (optional)
 */
public record UpdatePipelineRequest(

        @Size(max = 100, message = "Pipeline name must not exceed 100 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        Boolean isDefault,

        Integer displayOrder

) {

    public PipelineUpdateRequest toBllModel() {
        return new PipelineUpdateRequest(
                name != null ? name.trim() : null,
                description,
                isDefault,
                displayOrder
        );
    }
}
