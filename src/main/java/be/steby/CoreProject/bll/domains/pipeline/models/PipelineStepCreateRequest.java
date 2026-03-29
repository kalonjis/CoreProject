package be.steby.CoreProject.bll.domains.pipeline.models;

/**
 * BLL request model for adding a new {@link be.steby.CoreProject.dl.entities.crm.PipelineStep}
 * to an existing {@link be.steby.CoreProject.dl.entities.crm.Pipeline}.
 *
 * <p>The target pipeline is identified by its {@code publicId} at the service layer —
 * it is not part of this request to keep the model reusable.</p>
 *
 * <h3>Terminal flags</h3>
 * <p>{@code isWon} and {@code isLost} are mutually exclusive — both cannot be
 * {@code true} simultaneously. The service layer enforces this constraint and
 * also ensures only one Won step and one Lost step exist per pipeline.</p>
 *
 * <h3>Position</h3>
 * <p>Gaps are allowed (e.g. 0, 10, 20) to simplify future reordering.
 * If a step with the same position already exists in the pipeline, the service
 * layer shifts existing steps to make room.</p>
 *
 * <h3>Color</h3>
 * <p>Optional hex color code (e.g. {@code "#27ae60"}). If not provided, the UI
 * falls back to a neutral default. The service layer does not validate the format —
 * validation is handled at the PL level.</p>
 *
 * @param name           display name of the step (required)
 * @param color          hex color code for the Kanban column (optional)
 * @param position       zero-based display order within the pipeline (required)
 * @param isWon          marks this step as the terminal Won stage
 * @param isLost         marks this step as the terminal Lost stage
 * @param winProbability estimated win probability 0–100 (defaults to 50)
 */
public record PipelineStepCreateRequest(
        String name,
        String color,
        int position,
        boolean isWon,
        boolean isLost,
        int winProbability
) {}