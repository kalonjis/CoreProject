package be.steby.CoreProject.bll.domains.pipeline.models;

/**
 * BLL request model for partially updating an existing
 * {@link be.steby.CoreProject.dl.entities.crm.Pipeline}.
 *
 * <p>All fields are optional — only non-null values are applied.
 * Null fields are ignored and existing values are preserved (patch semantics).</p>
 *
 * <h3>Fields excluded from this request</h3>
 * <ul>
 *   <li>{@code pipelineSteps} — managed via dedicated step operations
 *       ({@code PipelineStepCreateRequest}, {@code PipelineStepUpdateRequest},
 *       {@code PipelineStepReorderRequest})</li>
 * </ul>
 *
 * <h3>Default pipeline</h3>
 * <p>If {@code isDefault} is set to {@code true}, the service layer must clear
 * the flag on all other pipelines before applying it to this one.
 * Setting {@code isDefault} to {@code false} on the current default pipeline
 * is allowed but leaves no default — the service layer logs a warning in this case.</p>
 *
 * @param name         new display name (optional)
 * @param description  new description (optional, pass empty string to clear)
 * @param isDefault    new default flag (optional)
 * @param displayOrder new display order in the UI selector (optional)
 */
public record PipelineUpdateRequest(
        String name,
        String description,
        Boolean isDefault,
        Integer displayOrder
) {}