package be.steby.CoreProject.bll.domains.crm.pipeline.models;

import java.util.List;

/**
 * BLL request model for atomically reordering the steps of a
 * {@link be.steby.CoreProject.dl.entities.crm.Pipeline}.
 *
 * <p>Triggered by a drag-and-drop action in the Kanban view.
 * The full ordered list of steps must be provided — the service layer
 * replaces all positions in a single transaction to avoid inconsistent
 * intermediate states.</p>
 *
 * <h3>Atomicity</h3>
 * <p>All positions are updated in one transaction. Partial updates
 * (providing only the moved step) are not supported — the caller must
 * always send the complete ordered list.</p>
 *
 * <h3>Validation</h3>
 * <p>The service layer verifies that:</p>
 * <ul>
 *   <li>All provided {@code stepPublicId} values belong to the target pipeline</li>
 *   <li>No step of the pipeline is missing from the list</li>
 *   <li>No duplicate positions are present</li>
 * </ul>
 *
 * @param orderedSteps the complete list of steps in their new order,
 *                     each entry carrying the step's public UUID and its new position
 */
public record PipelineStepReorderRequest(
        List<StepPosition> orderedSteps
) {

    /**
     * Maps a single {@link be.steby.CoreProject.dl.entities.crm.PipelineStep}
     * to its new position within the pipeline.
     *
     * @param stepPublicId public UUID of the step to reposition
     * @param newPosition  new zero-based position within the pipeline
     */
    public record StepPosition(
            String stepPublicId,
            int newPosition
    ) {}
}