package be.steby.CoreProject.pl.domains.pipeline.models.responses;

import be.steby.CoreProject.dl.entities.crm.Pipeline;

import java.time.Instant;
import java.util.List;

/**
 * Full response model for a {@link Pipeline}, including its ordered steps.
 *
 * @param publicId      public UUID of the pipeline
 * @param name          display name
 * @param description   optional description, or {@code null}
 * @param isDefault     {@code true} if this is the default pipeline for new deals
 * @param displayOrder  position in the UI pipeline selector
 * @param steps         ordered list of steps (by position ascending)
 * @param createdAt     timestamp of entity creation
 * @param updatedAt     timestamp of last update
 */
public record PipelineResponse(
        String publicId,
        String name,
        String description,
        boolean isDefault,
        int displayOrder,
        List<PipelineStepResponse> steps,
        Instant createdAt,
        Instant updatedAt
) {

    public static PipelineResponse fromEntity(Pipeline pipeline) {
        List<PipelineStepResponse> steps = pipeline.getPipelineSteps().stream()
                .map(PipelineStepResponse::fromEntity)
                .toList();

        return new PipelineResponse(
                pipeline.getPublicId(),
                pipeline.getName(),
                pipeline.getDescription(),
                pipeline.isDefault(),
                pipeline.getDisplayOrder(),
                steps,
                pipeline.getCreatedAt(),
                pipeline.getUpdatedAt()
        );
    }
}
