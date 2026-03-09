package be.steby.CoreProject.dl.enums.crm;

/**
 * Processing status of a {@link be.steby.CoreProject.dl.entities.Lead} in the CRM queue.
 *
 * <h3>Lifecycle</h3>
 * <pre>
 * NEW ──► IN_REVIEW ──► CONVERTED  (Contact + Deal created)
 *                   └──► REJECTED  (spam, out of scope, duplicate)
 * </pre>
 *
 * <p>Transitions are managed by the service layer.
 * A {@code CONVERTED} or {@code REJECTED} lead is terminal —
 * its status cannot be changed again.</p>
 */
public enum LeadStatus {

    /** Freshly submitted — not yet reviewed by the commercial team. */
    NEW,

    /** A commercial has opened the lead and is evaluating it. */
    IN_REVIEW,

    /** Lead was qualified and converted into a Contact. */
    CONVERTED,

    /** Lead was discarded (spam, out of scope, duplicate, etc.). */
    REJECTED
}