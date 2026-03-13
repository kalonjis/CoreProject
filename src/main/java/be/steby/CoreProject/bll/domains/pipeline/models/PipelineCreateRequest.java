package be.steby.CoreProject.bll.domains.pipeline.models;

/**
 * BLL request model for creating a new {@link be.steby.CoreProject.dl.entities.crm.Pipeline}.
 *
 * <p>Used when an admin creates a new sales pipeline. The pipeline is created
 * empty — {@link be.steby.CoreProject.dl.entities.crm.PipelineStep} are added
 * separately via dedicated step operations.</p>
 *
 * <h3>Default pipeline</h3>
 * <p>If {@code isDefault} is {@code true}, the service layer must clear the
 * {@code isDefault} flag on all other pipelines before setting it on this one.
 * Only one pipeline can be the default at a time.</p>
 *
 * <h3>Display order</h3>
 * <p>{@code displayOrder} controls the position of this pipeline in the UI
 * selector. Lower values appear first. Gaps are allowed (e.g. 0, 10, 20)
 * to simplify future reordering without renumbering all pipelines.</p>
 *
 * @param name         display name of the pipeline (required)
 * @param description  optional description of the pipeline's purpose or target market
 * @param isDefault    whether this pipeline should be the default for new deals
 * @param displayOrder position in the UI pipeline selector (0-based, lower = first)
 */
public record PipelineCreateRequest(
        String name,
        String description,
        boolean isDefault,
        int displayOrder
) {}