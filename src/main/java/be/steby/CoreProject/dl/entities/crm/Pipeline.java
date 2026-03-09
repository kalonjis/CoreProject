package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a sales pipeline — an ordered sequence of {@link PipelineStep} that
 * a {@link Deal} progresses through from qualification to closing.
 *
 * <p>A pipeline defines the commercial process for a specific type of sale.
 * For example, a cleaning services company might have a "B2B Cleaning" pipeline
 * with stages: Qualification → Quote sent → Negotiation → Won / Lost.</p>
 *
 * <h3>Inheritance</h3>
 * <p>Extends {@link BaseEntity} to inherit:</p>
 * <ul>
 *   <li>{@code id} — internal Long PK, never exposed via API</li>
 *   <li>{@code publicId} — UUID exposed in all public endpoints</li>
 *   <li>{@code createdBy}, {@code createdAt}, {@code updatedBy}, {@code updatedAt} — audit fields</li>
 * </ul>
 *
 * <h3>Relationships</h3>
 * <ul>
 *   <li>One {@code Pipeline} → many {@link PipelineStep} (ordered, cascade all)</li>
 *   <li>One {@code Pipeline} → many {@link Deal}</li>
 * </ul>
 *
 * <h3>Default pipeline</h3>
 * <p>The {@code isDefault} flag marks the pipeline automatically assigned to new deals
 * when no pipeline is explicitly chosen. Only one pipeline should have this flag set
 * at a time — enforced at the service layer.</p>
 *
 * <h3>Database indexes</h3>
 * <ul>
 *   <li>{@code idx_pipeline_public_id} — fast lookup by UUID (API)</li>
 *   <li>{@code idx_pipeline_default} — fast lookup of the default pipeline</li>
 * </ul>
 *
 * @see PipelineStep
 * @see Deal
 */
@Entity
@Table(
    name = "crm_pipeline",
    indexes = {
        @Index(name = "idx_pipeline_public_id", columnList = "public_id"),
        @Index(name = "idx_pipeline_default",   columnList = "is_default")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, exclude = {"stages"})
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class Pipeline extends BaseEntity<Long> {

    // =========================================================================
    // Identity
    // =========================================================================

    /**
     * Display name of the pipeline.
     *
     * <p>Required. Shown in the deal creation form and dashboard headers.</p>
     *
     * <p>Example: "B2B Cleaning", "Residential", "Partnership"</p>
     */
    @Column(name = "name", nullable = false, length = 100)
    @ToString.Include
    private String name;

    /**
     * Optional description of the pipeline's purpose or target market.
     *
     * <p>Shown in the pipeline configuration screen to help admins
     * distinguish between pipelines.</p>
     *
     * <p>Example: "For all B2B cleaning service contracts above 500€/month."</p>
     */
    @Column(name = "description", length = 500)
    private String description;

    // =========================================================================
    // Configuration
    // =========================================================================

    /**
     * Whether this is the default pipeline assigned to new deals.
     *
     * <p>Only one pipeline should be the default at a time.
     * The service layer must clear this flag on all other pipelines
     * before setting it on a new one.</p>
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    /**
     * Display order of this pipeline in the UI selector.
     *
     * <p>Lower values appear first. Managed by the admin in pipeline settings.</p>
     */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    // =========================================================================
    // Stages
    // =========================================================================

    /**
     * Ordered list of stages that compose this pipeline.
     *
     * <p>Stages are ordered by their {@link PipelineStep#getPosition()} field.
     * The list is loaded eagerly because pipelines are almost always
     * displayed with their stages in the UI.</p>
     *
     * <p>CascadeType.ALL ensures stages are persisted and deleted
     * along with their pipeline. OrphanRemoval cleans up removed stages.</p>
     */
    @OneToMany(
        mappedBy = "pipeline",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    @OrderBy("position ASC")
    @Builder.Default
    private List<PipelineStep> pipelineSteps = new ArrayList<>();


}