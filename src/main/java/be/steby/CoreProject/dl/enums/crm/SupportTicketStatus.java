package be.steby.CoreProject.dl.enums.crm;

/**
 * Lifecycle status of a {@link be.steby.CoreProject.dl.entities.crm.SupportTicket}.
 *
 * <h3>Transition rules</h3>
 * <pre>
 * OPEN ──► IN_PROGRESS ──► RESOLVED ──► CLOSED
 *  └──────────────────────────────────► CLOSED (direct close)
 * </pre>
 * <p>Once {@code CLOSED}, no further transitions are allowed.</p>
 */
public enum SupportTicketStatus {

    /** Ticket submitted and waiting to be picked up. */
    OPEN,

    /** Ticket is being actively handled by a team member. */
    IN_PROGRESS,

    /** Issue has been resolved — awaiting contact confirmation or auto-close. */
    RESOLVED,

    /** Ticket is closed — terminal state, no further updates allowed. */
    CLOSED
}
