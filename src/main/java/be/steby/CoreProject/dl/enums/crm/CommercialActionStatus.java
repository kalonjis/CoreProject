package be.steby.CoreProject.dl.enums.crm;

import be.steby.CoreProject.dl.entities.crm.CommercialAction;


/**
 * Lifecycle status of a {@link CommercialAction}.
 *
 * <h3>Transition rules</h3>
 * <pre>
 * PENDING ──► DONE
 *         └──► CANCELLED
 * </pre>
 * <p>Transitions are managed by the service layer.
 * A {@code DONE} or {@code CANCELLED} task cannot be reopened — a new task
 * must be created instead.</p>
 */
public enum CommercialActionStatus {

    /** Task has been created and is awaiting action. */
    PENDING,

    /** Task has been completed by the assigned commercial. */
    DONE,

    /** Task was explicitly cancelled — no action required. */
    CANCELLED
}