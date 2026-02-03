package be.steby.CoreProject.dl.enums;

/**
 * Enumeration representing recurrence patterns for calendar events.
 * 
 * <p>Defines how often an event repeats, supporting the most common
 * recurrence patterns used in calendar applications. This is a simplified
 * model suitable for most business use cases.</p>
 * 
 * <h3>Recurrence Patterns</h3>
 * <p>Each pattern defines a repeating cycle:</p>
 * <ul>
 *   <li><b>NONE:</b> Single occurrence, no repetition</li>
 *   <li><b>DAILY:</b> Repeats every day at the same time</li>
 *   <li><b>WEEKLY:</b> Repeats every 7 days (same day of week)</li>
 *   <li><b>MONTHLY:</b> Repeats every month (same date)</li>
 *   <li><b>YEARLY:</b> Repeats every year (same date)</li>
 * </ul>
 * 
 * <h3>iCalendar Mapping</h3>
 * <p>Converts to RFC 5545 RRULE (Recurrence Rule) format:</p>
 * <pre>{@code
 * BEGIN:VEVENT
 * DTSTART:20250210T140000Z
 * RRULE:FREQ=WEEKLY
 * ...
 * END:VEVENT
 * }</pre>
 * 
 * <h3>Common Use Cases by Pattern</h3>
 * <ul>
 *   <li><b>DAILY:</b>
 *     <ul>
 *       <li>Daily standup meetings</li>
 *       <li>Daily medication reminders</li>
 *       <li>Daily backup schedules</li>
 *     </ul>
 *   </li>
 *   <li><b>WEEKLY:</b>
 *     <ul>
 *       <li>Weekly team meetings (every Monday at 10am)</li>
 *       <li>Weekly reports due dates</li>
 *       <li>Recurring appointments (therapy, lessons)</li>
 *     </ul>
 *   </li>
 *   <li><b>MONTHLY:</b>
 *     <ul>
 *       <li>Monthly board meetings (1st Friday of month)</li>
 *       <li>Monthly billing cycles</li>
 *       <li>Subscription renewals</li>
 *     </ul>
 *   </li>
 *   <li><b>YEARLY:</b>
 *     <ul>
 *       <li>Birthdays and anniversaries</li>
 *       <li>Annual performance reviews</li>
 *       <li>Yearly tax deadlines</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h3>Current Limitations</h3>
 * <p>This simplified model does not support:</p>
 * <ul>
 *   <li>Custom intervals (every 2 weeks, every 3 months)</li>
 *   <li>End dates or occurrence counts (repeat until date X, or N times)</li>
 *   <li>Multiple days per week (every Mon/Wed/Fri)</li>
 *   <li>Complex patterns (2nd Tuesday of each month)</li>
 *   <li>Exception dates (EXDATE - skip specific occurrences)</li>
 * </ul>
 * 
 * <h3>Future Enhancement</h3>
 * <p>To support more complex recurrence patterns, consider:</p>
 * <pre>{@code
 * // Option 1: Store full RRULE string
 * @Column(name = "rrule")
 * private String rrule; // "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE,FR;COUNT=10"
 * 
 * // Option 2: Separate recurrence configuration entity
 * @OneToOne
 * private RecurrenceConfig recurrenceConfig;
 * }</pre>
 * 
 * <h3>Best Practices</h3>
 * <ul>
 *   <li>Always validate recurrence makes sense with event dates</li>
 *   <li>Consider timezone implications for recurring events</li>
 *   <li>Document how DST transitions affect recurring events</li>
 *   <li>For end-users, provide visual calendar preview of recurrence</li>
 * </ul>
 * 
 * @see be.steby.CoreProject.dl.entities.CalendarEvent#getRecurrence()
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5545#section-3.8.5.3">RFC 5545 Section 3.8.5.3 - Recurrence Rule</a>
 * @author Steby Corp
 * @version 1.0
 * @since 1.0
 */
public enum EventRecurrence {
    
    /**
     * No recurrence - single occurrence event.
     * 
     * <p>The event occurs only once at the specified date and time.
     * This is the default and most common type of calendar event.</p>
     * 
     * <p><b>Examples:</b></p>
     * <ul>
     *   <li>One-time meetings</li>
     *   <li>Specific appointments</li>
     *   <li>Single-day conferences</li>
     *   <li>Unique deadlines</li>
     * </ul>
     * 
     * <p><b>iCalendar:</b> No RRULE property included in exported .ics</p>
     * 
     * <p><b>Database:</b> This is the default value for new events</p>
     */
    NONE("None"),

    /**
     * Daily recurrence pattern.
     * 
     * <p>Event repeats every day at the same time. Useful for activities
     * that need to occur on every calendar day.</p>
     * 
     * <p><b>Behavior:</b></p>
     * <ul>
     *   <li>Repeats every 24 hours at the event's start time</li>
     *   <li>Continues indefinitely unless manually stopped</li>
     *   <li>Includes weekends by default</li>
     * </ul>
     * 
     * <p><b>Examples:</b></p>
     * <ul>
     *   <li>Daily standup meetings</li>
     *   <li>Daily medication reminders</li>
     *   <li>Daily report generation tasks</li>
     *   <li>Daily check-ins or logs</li>
     * </ul>
     * 
     * <p><b>iCalendar:</b> {@code RRULE:FREQ=DAILY}</p>
     * 
     * <p><b>Note:</b> For business days only (Mon-Fri), a custom
     * RRULE would be needed: {@code FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR}</p>
     */
    DAILY("Daily"),

    /**
     * Weekly recurrence pattern.
     * 
     * <p>Event repeats every 7 days on the same day of the week.
     * This is one of the most commonly used recurrence patterns.</p>
     * 
     * <p><b>Behavior:</b></p>
     * <ul>
     *   <li>Repeats on the same weekday (Mon, Tue, etc.)</li>
     *   <li>Preserves the time of day</li>
     *   <li>Automatically adjusts for month/year boundaries</li>
     * </ul>
     * 
     * <p><b>Examples:</b></p>
     * <ul>
     *   <li>Weekly team meetings (every Monday at 10:00)</li>
     *   <li>Weekly status reports (every Friday)</li>
     *   <li>Weekly fitness classes</li>
     *   <li>Weekly garbage collection days</li>
     * </ul>
     * 
     * <p><b>iCalendar:</b> {@code RRULE:FREQ=WEEKLY}</p>
     * 
     * <p><b>Timezone Note:</b> The recurrence is relative to the
     * start date's day of week, not absolute days. Event on
     * "Monday 10:00" stays "Monday 10:00" even after DST changes.</p>
     */
    WEEKLY("Weekly"),

    /**
     * Monthly recurrence pattern.
     * 
     * <p>Event repeats on the same date of each month. If the date
     * doesn't exist in a month (e.g., Feb 30), behavior depends on
     * calendar application implementation.</p>
     * 
     * <p><b>Behavior:</b></p>
     * <ul>
     *   <li>Repeats on the same day-of-month (1st, 15th, 31st, etc.)</li>
     *   <li>Preserves the time of day</li>
     *   <li>Handles month boundary cases (28/29/30/31 days)</li>
     * </ul>
     * 
     * <p><b>Examples:</b></p>
     * <ul>
     *   <li>Monthly billing dates (1st of each month)</li>
     *   <li>Monthly team meetings (3rd Friday)</li>
     *   <li>Monthly subscriptions renewal</li>
     *   <li>Monthly report deadlines</li>
     * </ul>
     * 
     * <p><b>iCalendar:</b> {@code RRULE:FREQ=MONTHLY}</p>
     * 
     * <p><b>Edge Cases:</b></p>
     * <ul>
     *   <li>Event on Jan 31 → Feb 28/29, Mar 31, Apr 30, etc.</li>
     *   <li>Some systems skip months without the date</li>
     *   <li>Others move to last day of month</li>
     *   <li>Document your application's behavior clearly</li>
     * </ul>
     * 
     * <p><b>Advanced Pattern Note:</b> For "3rd Monday of month",
     * would need: {@code FREQ=MONTHLY;BYDAY=3MO}</p>
     */
    MONTHLY("Monthly"),

    /**
     * Yearly recurrence pattern.
     * 
     * <p>Event repeats on the same date (month and day) each year.
     * Perfect for anniversaries, holidays, and annual events.</p>
     * 
     * <p><b>Behavior:</b></p>
     * <ul>
     *   <li>Repeats on the same month and day each year</li>
     *   <li>Preserves the time of day</li>
     *   <li>Automatically handles leap years</li>
     * </ul>
     * 
     * <p><b>Examples:</b></p>
     * <ul>
     *   <li>Birthdays and anniversaries</li>
     *   <li>Annual holidays (Christmas, New Year)</li>
     *   <li>Yearly performance reviews</li>
     *   <li>Annual subscription renewals</li>
     *   <li>Tax filing deadlines</li>
     * </ul>
     * 
     * <p><b>iCalendar:</b> {@code RRULE:FREQ=YEARLY}</p>
     * 
     * <p><b>Leap Year Note:</b> Events on Feb 29 occur only in
     * leap years. Different systems handle this differently:</p>
     * <ul>
     *   <li>Option 1: Skip non-leap years entirely</li>
     *   <li>Option 2: Move to Feb 28 in non-leap years</li>
     *   <li>Option 3: Move to Mar 1 in non-leap years</li>
     * </ul>
     */
    YEARLY("Yearly");

    /**
     * Human-readable display name for the recurrence pattern.
     * 
     * <p>Localized string (French) for UI display purposes.
     * Used in dropdown menus, event details, and recurrence summaries.</p>
     * 
     * <p><b>Internationalization Note:</b> For multi-language support,
     * consider using a message resource bundle instead of hardcoded strings.</p>
     */
    private final String displayName;

    /**
     * Constructor for EventRecurrence enum.
     * 
     * @param displayName Human-readable display name in French
     */
    EventRecurrence(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name of this recurrence pattern.
     * 
     * <p>Use this method when displaying the recurrence to end users
     * in the UI rather than using the enum name directly.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * EventRecurrence recurrence = EventRecurrence.WEEKLY;
     * System.out.println(recurrence.getDisplayName()); // Output: "Hebdomadaire"
     * }</pre>
     * 
     * @return The localized display name (currently French)
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this represents a recurring event.
     * 
     * <p>Convenience method to determine if an event repeats
     * or is a one-time occurrence.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * if (event.getRecurrence().isRecurring()) {
     *     // Show recurrence pattern in UI
     *     // Generate future occurrences
     * } else {
     *     // Simple one-time event
     * }
     * }</pre>
     * 
     * @return {@code true} if this is any pattern except NONE,
     *         {@code false} for NONE
     */
    public boolean isRecurring() {
        return this != NONE;
    }

    /**
     * Gets the iCalendar FREQ value for this recurrence pattern.
     * 
     * <p>Returns the FREQ parameter value for use in RRULE property
     * when exporting to iCalendar format.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * String freq = EventRecurrence.WEEKLY.getIcsFreq();
     * // Returns: "WEEKLY"
     * // Used in: RRULE:FREQ=WEEKLY
     * }</pre>
     * 
     * @return The RFC 5545 FREQ value, or empty string for NONE
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc5545#section-3.3.10">RFC 5545 Recurrence Rule</a>
     */
    public String getIcsFreq() {
        return switch (this) {
            case DAILY -> "DAILY";
            case WEEKLY -> "WEEKLY";
            case MONTHLY -> "MONTHLY";
            case YEARLY -> "YEARLY";
            case NONE -> "";
        };
    }
}