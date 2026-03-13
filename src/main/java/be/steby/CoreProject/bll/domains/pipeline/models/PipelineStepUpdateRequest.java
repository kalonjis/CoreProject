package be.steby.CoreProject.bll.domains.pipeline.models;

/**
 * BLL request model for partially updating an existing
 * {@link be.steby.CoreProject.dl.entities.crm.PipelineStep}.
 *
 * <p>All fields are optional — only non-null values are applied.
 * Null fields are ignored and existing values are preserved (patch semantics).</p>
 *
 * <h3>Fields excluded from this request</h3>
 * <ul>
 *   <li>{@code position} — managed via {@code PipelineStepReorderRequest}
 *       to keep reordering atomic across all steps of a pipeline</li>
 *   <li>{@code isWon} / {@code isLost} — terminal flags are intentionally
 *       excluded from patch updates. Changing them has side effects on open
 *       deals and requires a dedicated service operation.</li>
 * </ul>
 *
 * @param name   new display name of the step (optional)
 * @param color  new hex color code for the Kanban column (optional, pass empty string to clear)
 */
public record PipelineStepUpdateRequest(
        String name,
        String color
) {}