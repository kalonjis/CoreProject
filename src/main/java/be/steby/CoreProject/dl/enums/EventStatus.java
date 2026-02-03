package be.steby.CoreProject.dl.enums;

/**
 * Enumeration representing the status of a calendar event.
 * 
 * <p>This enum defines the lifecycle states an event can be in, matching
 * the standard iCalendar STATUS property values defined in RFC 5545.</p>
 * 
 * <h3>Status Transitions</h3>
 * <p>Typical event lifecycle:</p>
 * <pre>
 * TENTATIVE → CONFIRMED → CANCELLED
 *     ↓           ↓
 *  CANCELLED  CANCELLED
 * </pre>
 * 
 * <h3>Use Cases by Status</h3>
 * <ul>
 *   <li><b>TENTATIVE:</b>
 *     <ul>
 *       <li>Meeting invites pending confirmation</li>
 *       <li>Events with uncertain dates/times</li>
 *       <li>Placeholder events subject to change</li>
 *     </ul>
 *   </li>
 *   <li><b>CONFIRMED:</b>
 *     <ul>
 *       <li>Finalized meetings and appointments</li>
 *       <li>Events ready for reminders</li>
 *       <li>Standard operational state</li>
 *     </ul>
 *   </li>
 *   <li><b>CANCELLED:</b>
 *     <ul>
 *       <li>Events that won't occur</li>
 *       <li>Kept for historical/audit purposes</li>
 *       <li>Excluded from "upcoming events" views</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h3>iCalendar Compatibility</h3>
 * <p>Maps directly to RFC 5545 STATUS property:</p>
 * <pre>{@code
 * BEGIN:VEVENT
 * STATUS:CONFIRMED
 * ...
 * END:VEVENT
 * }</pre>
 * 
 * <h3>Business Rules</h3>
 * <ul>
 *   <li>Only CONFIRMED events appear in "upcoming events" queries</li>
 *   <li>CANCELLED events are soft-deleted (kept for audit trail)</li>
 *   <li>Reminders are not sent for CANCELLED or TENTATIVE events</li>
 *   <li>Status changes should trigger domain events for audit logging</li>
 * </ul>
 * 
 * @see be.steby.CoreProject.dl.entities.CalendarEvent#getStatus()
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5545#section-3.8.1.11">RFC 5545 Section 3.8.1.11 - Status</a>
 * @author Steby Corp
 * @version 1.0
 * @since 1.0
 */
public enum EventStatus {
    
    /**
     * Event is tentative or provisional.
     * 
     * <p>Indicates the event has not been definitively scheduled.
     * The event may still change in terms of date, time, location,
     * or may be cancelled altogether.</p>
     * 
     * <p><b>Common Scenarios:</b></p>
     * <ul>
     *   <li>Initial calendar blocking before confirmation</li>
     *   <li>Meeting invites awaiting response</li>
     *   <li>Events dependent on external factors</li>
     *   <li>Placeholder reservations</li>
     * </ul>
     * 
     * <p><b>Display:</b> Often shown with different styling
     * (e.g., dashed border, lighter color) to indicate uncertainty.</p>
     * 
     * <p><b>iCalendar:</b> {@code STATUS:TENTATIVE}</p>
     */
    TENTATIVE("Tentative"),

    /**
     * Event is confirmed and finalized.
     * 
     * <p>This is the standard operational status for events that
     * are definitely scheduled and expected to occur as planned.</p>
     * 
     * <p><b>Characteristics:</b></p>
     * <ul>
     *   <li>Date, time, and location are finalized</li>
     *   <li>Participants are notified and expected</li>
     *   <li>Reminders will be sent as configured</li>
     *   <li>Appears in all standard calendar views</li>
     * </ul>
     * 
     * <p><b>Default Status:</b> New events are typically created
     * with this status unless explicitly set otherwise.</p>
     * 
     * <p><b>Display:</b> Normal calendar event appearance with
     * full color and solid borders.</p>
     * 
     * <p><b>iCalendar:</b> {@code STATUS:CONFIRMED}</p>
     */
    CONFIRMED("Confirmé"),

    /**
     * Event has been cancelled.
     * 
     * <p>Indicates the event will not occur. The event record is
     * retained for historical purposes and audit trails rather than
     * being permanently deleted.</p>
     * 
     * <p><b>Implications:</b></p>
     * <ul>
     *   <li>Excluded from "upcoming events" queries</li>
     *   <li>No reminders will be sent</li>
     *   <li>May still appear in "all events" or historical views</li>
     *   <li>Can include cancellation notes in description</li>
     * </ul>
     * 
     * <p><b>Soft Delete Pattern:</b> This status implements a soft
     * delete approach, preserving data for:</p>
     * <ul>
     *   <li>Audit and compliance requirements</li>
     *   <li>Historical record keeping</li>
     *   <li>Analytics and reporting</li>
     *   <li>Potential event rescheduling</li>
     * </ul>
     * 
     * <p><b>Display:</b> Often shown with strikethrough text,
     * grayed out, or marked with a cancellation icon.</p>
     * 
     * <p><b>iCalendar:</b> {@code STATUS:CANCELLED}</p>
     * 
     * <p><b>Note:</b> For permanent deletion, use the delete operation
     * in {@link be.steby.CoreProject.bll.domains.calendar.services.CalendarEventService}
     * instead of setting this status.</p>
     */
    CANCELLED("Annulé");

    /**
     * Human-readable display name for the status.
     * 
     * <p>Localized string (French) for UI display purposes.
     * Used in dropdown menus, status badges, and event details.</p>
     * 
     * <p><b>Internationalization Note:</b> For multi-language support,
     * consider using a message resource bundle instead of hardcoded strings.</p>
     */
    private final String displayName;

    /**
     * Constructor for EventStatus enum.
     * 
     * @param displayName Human-readable display name in French
     */
    EventStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name of this status.
     * 
     * <p>Use this method when displaying the status to end users
     * in the UI rather than using the enum name directly.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * EventStatus status = EventStatus.CONFIRMED;
     * System.out.println(status.getDisplayName()); // Output: "Confirmé"
     * }</pre>
     * 
     * @return The localized display name (currently French)
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this status represents a cancelled event.
     * 
     * <p>Convenience method for conditional logic that needs to
     * treat cancelled events differently.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * if (event.getStatus().isCancelled()) {
     *     // Don't send reminder
     *     // Exclude from upcoming events list
     * }
     * }</pre>
     * 
     * @return {@code true} if status is CANCELLED, {@code false} otherwise
     */
    public boolean isCancelled() {
        return this == CANCELLED;
    }

    /**
     * Checks if this status represents a confirmed event.
     * 
     * <p>Convenience method to identify events that are finalized
     * and should be treated as definite appointments.</p>
     * 
     * @return {@code true} if status is CONFIRMED, {@code false} otherwise
     */
    public boolean isConfirmed() {
        return this == CONFIRMED;
    }

    /**
     * Checks if this status represents a tentative event.
     * 
     * <p>Convenience method to identify events that are not yet
     * finalized and may still change.</p>
     * 
     * @return {@code true} if status is TENTATIVE, {@code false} otherwise
     */
    public boolean isTentative() {
        return this == TENTATIVE;
    }
}