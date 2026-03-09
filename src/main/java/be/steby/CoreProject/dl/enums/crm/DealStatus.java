package be.steby.CoreProject.dl.enums.crm;

import be.steby.CoreProject.dl.entities.crm.PipelineStep;

/**
 * Lifecycle status of a {@link be.steby.CoreProject.dl.entities.crm.Deal}.
 *
 * <p>Transitions are driven by stage changes managed by the service layer.
 * When a deal enters a terminal {@link PipelineStep},
 * the service updates this status and records {@code closedAt}.</p>
 *
 * <h3>Transition rules</h3>
 * <ul>
 *   <li>{@code OPEN} → {@code WON}  : deal enters a stage where {@code isWon = true}</li>
 *   <li>{@code OPEN} → {@code LOST} : deal enters a stage where {@code isLost = true}</li>
 *   <li>{@code WON} / {@code LOST}  : terminal — no further transitions allowed</li>
 * </ul>
 */
public enum DealStatus {

    /** Deal is active and progressing through the pipeline. */
    OPEN,

    /** Deal was successfully closed — contract signed. */
    WON,

    /** Deal was lost — prospect declined or went with a competitor. */
    LOST
}