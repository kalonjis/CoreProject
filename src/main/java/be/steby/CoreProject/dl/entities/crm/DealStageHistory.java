package be.steby.CoreProject.dl.entities.crm;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Append-only log of deal stage transitions.
 *
 * <p>One entry is inserted each time a {@link Deal} enters a {@link PipelineStep}.
 * The previous open entry is closed ({@code exitedAt} set) before the new one
 * is created, so the full stage history of a deal can be reconstructed.</p>
 *
 * <h3>Lifecycle</h3>
 * <ul>
 *   <li>Deal created → one entry inserted for the initial stage ({@code exitedAt = null})</li>
 *   <li>Deal moved → current open entry closed, new entry opened</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <p>Used by {@code PipelineServiceImpl} to compute per-stage statistics:</p>
 * <ul>
 *   <li>Total deals that ever entered a stage ({@code dealsEntered})</li>
 *   <li>Average time spent in a stage ({@code avgDaysInStage})</li>
 *   <li>Stage-to-stage conversion rates</li>
 * </ul>
 *
 * <p>Note: does not extend {@link be.steby.CoreProject.dl.entities.BaseEntity} —
 * this is an audit log, not a domain entity requiring publicId or audit fields.</p>
 */
@Entity
@Table(
    name = "deal_stage_history",
    indexes = {
        @Index(name = "idx_dsh_deal",    columnList = "deal_id"),
        @Index(name = "idx_dsh_step",    columnList = "pipeline_step_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DealStageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The deal whose stage transition is recorded.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id", nullable = false)
    private Deal deal;

    /**
     * The pipeline step the deal entered.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_step_id", nullable = false)
    private PipelineStep pipelineStep;

    /**
     * When the deal entered this stage.
     */
    @Column(name = "entered_at", nullable = false)
    private Instant enteredAt;

    /**
     * When the deal left this stage.
     *
     * <p>{@code null} if the deal is still at this stage (or has been closed here).</p>
     */
    @Column(name = "exited_at")
    private Instant exitedAt;
}
