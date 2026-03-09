package be.steby.CoreProject.dl.enums.crm;

/**
 * Lifecycle status of a {@link be.steby.CoreProject.dl.entities.crm.Contact} in the CRM.
 *
 * <p>Transitions are managed by the service layer and driven by deal outcomes
 * and manual updates by the commercial team.</p>
 *
 * <h3>Typical flow</h3>
 * <pre>
 * NEW ──► ENGAGED ──► QUALIFIED ──► CLIENT
 *                                      │
 *              LOST ◄──────────────────┘ (if all deals lost)
 *              INACTIVE (no activity for a long period)
 * </pre>
 */
public enum ContactStatus {

    /** Freshly converted from a lead — not yet contacted. */
    NEW,

    /** First contact established — exchange in progress. */
    ENGAGED,

    /** Needs confirmed — actively working toward a deal. */
    QUALIFIED,

    /** At least one deal won — active paying client. */
    CLIENT,

    /** All deals lost or contact explicitly disqualified. */
    LOST,

    /** No activity for an extended period — dormant. */
    INACTIVE
}