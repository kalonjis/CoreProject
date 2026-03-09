package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a single step within a {@link Pipeline}.
 *
 * <p>Stages define the progression path of a {@link Deal}. Each stage has a
 * position that determines its order in the pipeline, and terminal flags
 * ({@code isWon}, {@code isLost}) that mark deal-closing stages.</p>
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
 *   <li>Many {@code Stage} → one {@link Pipeline} (owning side)</li>
 *   <li>One {@code Stage} → many {@link Deal} (deals currently at this stage)</li>
 * </ul>
 *
 * <h3>Terminal stages</h3>
 * <p>A pipeline typically ends with two terminal stages:</p>
 * <ul>
 *   <li>A <b>Won</b> stage ({@code isWon = true}) — deal successfully closed.</li>
 *   <li>A <b>Lost</b> stage ({@code isLost = true}) — deal abandoned or rejected.</li>
 * </ul>
 * <p>Both flags cannot be {@code true} simultaneously — enforced at the service layer.</p>
 *
 * <h3>Example pipeline</h3>
 * <pre>
 * pos 0 — Qualification   (isWon=false, isLost=false)
 * pos 1 — Quote sent      (isWon=false, isLost=false)
 * pos 2 — Negotiation     (isWon=false, isLost=false)
 * pos 3 — Won  ✅         (isWon=true,  isLost=false)
 * pos 4 — Lost ❌         (isWon=false, isLost=true)
 * </pre>
 *
 * <h3>Database indexes</h3>
 * <ul>
 *   <li>{@code idx_stage_public_id} — fast lookup by UUID (API)</li>
 *   <li>{@code idx_stage_pipeline}  — fetch all stages of a pipeline</li>
 * </ul>
 *
 * @see Pipeline
 * @see Deal
 */
@Entity
@Table(
        name = "crm_pipeline_step", // Renommé pour cohérence
        indexes = {
                @Index(name = "idx_pipeline_step_public_id", columnList = "public_id"),
                @Index(name = "idx_pipeline_step_pipeline",  columnList = "pipeline_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, exclude = {"pipeline"})
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class PipelineStep extends BaseEntity<Long> {

    // =========================================================================
    // Identity
    // =========================================================================

    /**
     * Display name of the stage.
     *
     * <p>Required. Shown on deal cards and in the pipeline Kanban view.</p>
     *
     * <p>Example: "Qualification", "Quote sent", "Negotiation", "Won", "Lost"</p>
     */
    @Column(name = "name", nullable = false, length = 100)
    @ToString.Include
    private String name;

    /**
     * Hex color code used to visually distinguish the stage in the UI.
     *
     * <p>Optional. Defaults to a neutral grey if not set.
     * Green is conventionally used for Won, red for Lost.</p>
     *
     * <p>Example: "#27ae60" (green), "#e74c3c" (red), "#3498db" (blue)</p>
     */
    @Column(name = "color", length = 7)
    private String color;

    // =========================================================================
    // Ordering
    // =========================================================================

    /**
     * Zero-based position of this stage within its pipeline.
     *
     * <p>Required. Determines the left-to-right display order in the Kanban view.
     * Managed by the admin in pipeline settings. Gaps are allowed (e.g. 0, 10, 20)
     * to make reordering easier without renumbering all stages.</p>
     */
    @Column(name = "position", nullable = false)
    @ToString.Include
    private int position;

    // =========================================================================
    // Terminal flags
    // =========================================================================

    /**
     * Marks this stage as the "Won" terminal stage.
     *
     * <p>When a deal reaches this stage, its status is set to {@code WON}
     * and {@code closedAt} is recorded. Only one Won stage per pipeline
     * is recommended — enforced at the service layer.</p>
     */
    @Column(name = "is_won", nullable = false)
    @Builder.Default
    private boolean isWon = false;

    /**
     * Marks this stage as the "Lost" terminal stage.
     *
     * <p>When a deal reaches this stage, its status is set to {@code LOST}
     * and {@code closedAt} is recorded. Only one Lost stage per pipeline
     * is recommended — enforced at the service layer.</p>
     */
    @Column(name = "is_lost", nullable = false)
    @Builder.Default
    private boolean isLost = false;

    // =========================================================================
    // Relationship
    // =========================================================================

    /**
     * The pipeline this stage belongs to.
     *
     * <p>Required. Owning side of the {@link Pipeline#getPipelineSteps()} relationship.
     * Loaded lazily — the pipeline is not needed when querying deals by stage.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id", nullable = false)
    private Pipeline pipeline;

    // =========================================================================
    // Utility methods
    // =========================================================================

    /**
     * Returns {@code true} if this is a terminal stage (either Won or Lost).
     *
     * <p>Used by the service layer to trigger deal-closing logic
     * (set {@code closedAt}, update {@link ContactStatus}, etc.).</p>
     *
     * @return true if {@code isWon} or {@code isLost} is set
     */
    public boolean isTerminal() {
        return this.isWon || this.isLost;
    }
}