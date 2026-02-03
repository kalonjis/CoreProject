package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.EventRecurrence;
import be.steby.CoreProject.dl.enums.EventStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity representing a calendar event in the system.
 * 
 * <p>This entity stores information about scheduled events, meetings, reminders,
 * and other calendar-related activities. It supports both timed and all-day events,
 * as well as recurring event patterns.</p>
 * 
 * <h3>Inheritance</h3>
 * <p>Extends {@link BaseEntity} to inherit:</p>
 * <ul>
 *   <li>Auto-generated internal {@code id} (Long) for database operations</li>
 *   <li>Auto-generated {@code publicId} (String) for secure API exposure</li>
 *   <li>Audit fields: {@code createdBy}, {@code createdAt}, {@code updatedBy}, {@code updatedAt}</li>
 * </ul>
 * 
 * <h3>Design Decisions</h3>
 * <ul>
 *   <li><b>Owner Reference:</b> Uses {@code ownerPublicId} instead of direct FK
 *       for flexibility and to avoid tight coupling with User entity</li>
 *   <li><b>Date Storage:</b> Uses {@link Instant} for timezone-independent storage;
 *       timezone conversion handled at presentation layer</li>
 *   <li><b>All-Day Events:</b> Supported via {@code allDay} flag; times are still
 *       stored but should be set to start/end of day</li>
 *   <li><b>Recurrence:</b> Simplified recurrence model suitable for most use cases;
 *       can be extended to full RRULE format if needed</li>
 * </ul>
 * 
 * <h3>Export Compatibility</h3>
 * <p>This entity structure is designed to be easily exportable to:</p>
 * <ul>
 *   <li>iCalendar (.ics) format - RFC 5545 compliant</li>
 *   <li>Google Calendar via URL or API</li>
 *   <li>Microsoft Outlook via URL or API</li>
 *   <li>Apple Calendar and other iCalendar-compatible applications</li>
 * </ul>
 * 
 * <h3>Database Indexes</h3>
 * <p>Optimized for common query patterns:</p>
 * <ul>
 *   <li>{@code idx_calendar_event_public_id} - Fast lookups by public ID (API)</li>
 *   <li>{@code idx_calendar_event_owner} - Fast retrieval of user's events</li>
 *   <li>{@code idx_calendar_event_start} - Date range queries and sorting</li>
 *   <li>{@code idx_calendar_event_status} - Filtering by event status</li>
 * </ul>
 * 
 * <h3>Example Usage</h3>
 * <pre>{@code
 * CalendarEvent event = CalendarEvent.builder()
 *     .ownerPublicId(user.getPublicId())
 *     .title("Team Meeting")
 *     .description("Weekly sync-up")
 *     .startDateTime(Instant.parse("2025-02-10T14:00:00Z"))
 *     .endDateTime(Instant.parse("2025-02-10T15:00:00Z"))
 *     .location("Conference Room A")
 *     .status(EventStatus.CONFIRMED)
 *     .recurrence(EventRecurrence.WEEKLY)
 *     .build();
 * }</pre>
 * 
 * @see BaseEntity
 * @see EventStatus
 * @see EventRecurrence
 * @author Steby Corp
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "calendar_event", indexes = {
    @Index(name = "idx_calendar_event_public_id", columnList = "public_id"),
    @Index(name = "idx_calendar_event_owner", columnList = "owner_public_id"),
    @Index(name = "idx_calendar_event_start", columnList = "start_date_time"),
    @Index(name = "idx_calendar_event_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class CalendarEvent extends BaseEntity<Long> {

    // =========================================================================
    // Ownership
    // =========================================================================

    /**
     * Public ID of the event owner (User entity).
     * 
     * <p>Uses publicId instead of a foreign key relationship for several reasons:</p>
     * <ul>
     *   <li>Decouples calendar domain from user domain</li>
     *   <li>Allows flexible ownership models (e.g., shared calendars in future)</li>
     *   <li>Prevents accidental exposure of internal user IDs</li>
     *   <li>Simplifies serialization and API responses</li>
     * </ul>
     * 
     * <p><b>Security Note:</b> This field should always be validated against
     * the authenticated user's publicId before modifications.</p>
     * 
     * @see be.steby.CoreProject.dl.entities.User#getPublicId()
     */
    @Column(name = "owner_public_id", nullable = false, length = 256)
    private String ownerPublicId;

    // =========================================================================
    // Event Details
    // =========================================================================

    /**
     * Title or summary of the event.
     * 
     * <p>This is the primary display name shown in calendar views and event lists.
     * Should be concise but descriptive enough to identify the event at a glance.</p>
     * 
     * <p><b>Length Limit:</b> 200 characters to ensure compatibility with
     * most calendar systems and prevent UI overflow.</p>
     * 
     * <p><b>iCalendar Mapping:</b> Maps to the SUMMARY property in .ics files.</p>
     * 
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc5545#section-3.8.1.12">RFC 5545 SUMMARY</a>
     */
    @Column(name = "title", nullable = false, length = 200)
    @ToString.Include
    private String title;

    /**
     * Detailed description of the event.
     * 
     * <p>Optional field for additional context, agenda, notes, or instructions
     * related to the event. Can contain formatted text, URLs, or meeting details.</p>
     * 
     * <p><b>Storage:</b> TEXT column to support longer descriptions (typically
     * up to 65,535 characters in MySQL/PostgreSQL).</p>
     * 
     * <p><b>iCalendar Mapping:</b> Maps to the DESCRIPTION property in .ics files.</p>
     * Special characters (commas, semicolons, newlines) are automatically escaped
     * during export.
     * 
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc5545#section-3.8.1.5">RFC 5545 DESCRIPTION</a>
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Physical or virtual location of the event.
     * 
     * <p>Can contain:</p>
     * <ul>
     *   <li>Physical addresses: "123 Main St, City, Country"</li>
     *   <li>Room names: "Conference Room A", "Building 2, Floor 3"</li>
     *   <li>Virtual meeting links: "https://meet.google.com/abc-defg-hij"</li>
     *   <li>Phone numbers: "+1-555-123-4567"</li>
     * </ul>
     * 
     * <p><b>iCalendar Mapping:</b> Maps to the LOCATION property in .ics files.</p>
     * 
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc5545#section-3.8.1.7">RFC 5545 LOCATION</a>
     */
    @Column(name = "location", length = 300)
    private String location;

    // =========================================================================
    // Date & Time
    // =========================================================================

    /**
     * Event start date and time in UTC.
     * 
     * <p>Stored as {@link Instant} for timezone-independent storage. The actual
     * timezone display is handled at the presentation layer based on user preferences.</p>
     * 
     * <p><b>For All-Day Events:</b> Set to the start of the day in UTC
     * (e.g., 2025-02-15T00:00:00Z). The {@code allDay} flag determines
     * how this is displayed and exported.</p>
     * 
     * <p><b>iCalendar Mapping:</b></p>
     * <ul>
     *   <li>Timed events: {@code DTSTART:20250215T140000Z}</li>
     *   <li>All-day events: {@code DTSTART;VALUE=DATE:20250215}</li>
     * </ul>
     * 
     * @see #allDay
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc5545#section-3.8.2.4">RFC 5545 DTSTART</a>
     */
    @Column(name = "start_date_time", nullable = false)
    @ToString.Include
    private Instant startDateTime;

    /**
     * Event end date and time in UTC.
     * 
     * <p>Must be after or equal to {@link #startDateTime}. This is validated
     * at the service layer before persistence.</p>
     * 
     * <p><b>For All-Day Events:</b> Set to the end of the last day
     * (e.g., 2025-02-15T23:59:59Z) or start of next day (2025-02-16T00:00:00Z).</p>
     * 
     * <p><b>Duration Calculation:</b> Duration = endDateTime - startDateTime</p>
     * 
     * <p><b>iCalendar Mapping:</b></p>
     * <ul>
     *   <li>Timed events: {@code DTEND:20250215T160000Z}</li>
     *   <li>All-day events: {@code DTEND;VALUE=DATE:20250216}</li>
     * </ul>
     * 
     * @see #startDateTime
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc5545#section-3.8.2.2">RFC 5545 DTEND</a>
     */
    @Column(name = "end_date_time", nullable = false)
    private Instant endDateTime;

    /**
     * Flag indicating whether this is an all-day event.
     * 
     * <p>When {@code true}:</p>
     * <ul>
     *   <li>Event is displayed without specific times in calendar views</li>
     *   <li>Exported with DATE format instead of DATE-TIME in .ics</li>
     *   <li>Start/end times should still be set but are ignored for display</li>
     *   <li>Typically spans entire day(s) in the user's local timezone</li>
     * </ul>
     * 
     * <p>When {@code false}:</p>
     * <ul>
     *   <li>Event is displayed with specific start and end times</li>
     *   <li>Exported with full DATE-TIME format in .ics</li>
     *   <li>Timezone conversions applied based on user preferences</li>
     * </ul>
     * 
     * <p><b>Default:</b> {@code false} (timed event)</p>
     */
    @Column(name = "all_day", nullable = false)
    private boolean allDay = false;

    // =========================================================================
    // Status & Recurrence
    // =========================================================================

    /**
     * Current status of the event.
     * 
     * <p>Determines how the event is treated in calendar applications:</p>
     * <ul>
     *   <li><b>TENTATIVE:</b> Event is proposed but not confirmed; may change</li>
     *   <li><b>CONFIRMED:</b> Event is confirmed and will happen as scheduled</li>
     *   <li><b>CANCELLED:</b> Event has been cancelled but record is kept for history</li>
     * </ul>
     * 
     * <p><b>Default:</b> {@link EventStatus#CONFIRMED}</p>
     * 
     * <p><b>Business Rules:</b></p>
     * <ul>
     *   <li>CANCELLED events are excluded from "upcoming events" queries</li>
     *   <li>Status changes should emit domain events for audit logging</li>
     *   <li>Reminders are not sent for CANCELLED events</li>
     * </ul>
     * 
     * <p><b>iCalendar Mapping:</b> Maps directly to STATUS property
     * (TENTATIVE, CONFIRMED, CANCELLED)</p>
     * 
     * @see EventStatus
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc5545#section-3.8.1.11">RFC 5545 STATUS</a>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ToString.Include
    private EventStatus status = EventStatus.CONFIRMED;

    /**
     * Recurrence pattern for repeating events.
     * 
     * <p>Defines how often the event repeats:</p>
     * <ul>
     *   <li><b>NONE:</b> Single occurrence, no repetition</li>
     *   <li><b>DAILY:</b> Repeats every day</li>
     *   <li><b>WEEKLY:</b> Repeats every week on the same day</li>
     *   <li><b>MONTHLY:</b> Repeats every month on the same date</li>
     *   <li><b>YEARLY:</b> Repeats every year on the same date</li>
     * </ul>
     * 
     * <p><b>Default:</b> {@link EventRecurrence#NONE}</p>
     * 
     * <p><b>Current Limitations:</b></p>
     * <ul>
     *   <li>Simplified model - no custom intervals (every 2 weeks, etc.)</li>
     *   <li>No end date/count for recurrence (infinite by default)</li>
     *   <li>No exclusion dates (EXDATE)</li>
     * </ul>
     * 
     * <p><b>Future Enhancement:</b> Can be extended to support full RRULE
     * format with custom intervals, end conditions, and exceptions.</p>
     * 
     * <p><b>iCalendar Mapping:</b> Converted to RRULE property
     * (e.g., {@code RRULE:FREQ=WEEKLY})</p>
     * 
     * @see EventRecurrence
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc5545#section-3.8.5.3">RFC 5545 RRULE</a>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence", nullable = false, length = 20)
    private EventRecurrence recurrence = EventRecurrence.NONE;

    // =========================================================================
    // Optional Features
    // =========================================================================

    /**
     * Color code for visual calendar display (optional).
     * 
     * <p>Allows users to categorize and visually distinguish events in
     * calendar views. Stored as hex color code.</p>
     * 
     * <p><b>Format:</b> 7-character string including '#' (e.g., "#FF5733")</p>
     * <ul>
     *   <li>Valid: "#FF5733", "#3498DB", "#2ECC71"</li>
     *   <li>Invalid: "FF5733", "red", "#F00"</li>
     * </ul>
     * 
     * <p><b>Validation:</b> Format validation performed at service layer</p>
     * 
     * <p><b>UI Usage:</b></p>
     * <ul>
     *   <li>Background color for event blocks in calendar grid</li>
     *   <li>Border or label color in list views</li>
     *   <li>Category indicator in mini-calendars</li>
     * </ul>
     * 
     * <p><b>Note:</b> Not part of standard iCalendar format but can be
     * included as a custom property (X-APPLE-CALENDAR-COLOR, etc.)</p>
     */
    @Column(name = "color_code", length = 7)
    private String colorCode;

    /**
     * Email reminder time in minutes before event start (optional).
     * 
     * <p>Defines when to send a reminder notification before the event begins:</p>
     * <ul>
     *   <li>{@code null} - No reminder</li>
     *   <li>{@code 0} - Reminder at event start time</li>
     *   <li>{@code 15} - Reminder 15 minutes before event</li>
     *   <li>{@code 60} - Reminder 1 hour before event</li>
     *   <li>{@code 1440} - Reminder 1 day before event</li>
     * </ul>
     * 
     * <p><b>Common Values:</b></p>
     * <ul>
     *   <li>5 minutes: Quick reminder just before</li>
     *   <li>15 minutes: Standard pre-meeting reminder</li>
     *   <li>30 minutes: Time to prepare and travel</li>
     *   <li>60 minutes: One hour notice</li>
     *   <li>1440 minutes (24 hours): Day-before reminder</li>
     * </ul>
     * 
     * <p><b>Implementation Note:</b> Actual reminder sending should be
     * handled by a scheduled background job that checks upcoming events
     * and their reminder settings.</p>
     * 
     * <p><b>iCalendar Mapping:</b> Can be exported as VALARM component
     * with TRIGGER property</p>
     * 
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc5545#section-3.6.6">RFC 5545 VALARM</a>
     */
    @Column(name = "reminder_minutes")
    private Integer reminderMinutes;
}