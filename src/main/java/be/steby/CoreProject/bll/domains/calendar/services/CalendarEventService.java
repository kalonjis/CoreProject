package be.steby.CoreProject.bll.domains.calendar.services;

import be.steby.CoreProject.bll.common.exceptions.OwnershipException;
import be.steby.CoreProject.bll.domains.calendar.models.CalendarEventCreateRequest;
import be.steby.CoreProject.bll.domains.calendar.models.CalendarEventUpdateRequest;
import be.steby.CoreProject.bll.domains.calendar.exceptions.CalendarEventNotFoundException;
import be.steby.CoreProject.dl.entities.CalendarEvent;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dal.CalendarEventRepository ;
import be.steby.CoreProject.dl.enums.EventStatus;

import java.time.Instant;
import java.util.List;

/**
 * Service interface for calendar event business logic operations.
 * 
 * <p>This interface defines the contract for managing calendar events in the system.
 * It encapsulates all business logic related to event lifecycle management,
 * validation, and access control.</p>
 * 
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li><b>Event Lifecycle:</b> Create, read, update, cancel, and delete events</li>
 *   <li><b>Validation:</b> Business rule validation (dates, ownership, etc.)</li>
 *   <li><b>Access Control:</b> Ensure users can only access their own events</li>
 *   <li><b>Domain Events:</b> Emit events for audit logging and notifications</li>
 *   <li><b>Data Retrieval:</b> Query events by various criteria</li>
 * </ul>
 * 
 * <h3>Design Principles</h3>
 * <ul>
 *   <li><b>Separation of Concerns:</b> Business logic isolated from data access</li>
 *   <li><b>Domain-Driven Design:</b> Works with domain entities and models</li>
 *   <li><b>Transaction Management:</b> Implementation handles transaction boundaries</li>
 *   <li><b>Security First:</b> All operations verify user ownership</li>
 * </ul>
 * 
 * <h3>Transaction Management</h3>
 * <p>The implementing class should use {@code @Transactional} annotations:</p>
 * <ul>
 *   <li>Read operations: {@code @Transactional(readOnly = true)}</li>
 *   <li>Write operations: {@code @Transactional}</li>
 * </ul>
 * 
 * <h3>Error Handling</h3>
 * <p>Methods may throw:</p>
 * <ul>
 *   <li>{@code CalendarEventNotFoundException} - Event not found by public ID</li>
 *   <li>{@code UnauthorizedOperationException} - User doesn't own the event</li>
 *   <li>{@code IllegalArgumentException} - Invalid input data</li>
 *   <li>{@code ValidationException} - Business rule violations</li>
 * </ul>
 * 
 * <h3>Example Implementation Flow</h3>
 * <pre>{@code
 * @Service
 * @Transactional(readOnly = true)
 * public class CalendarEventServiceImpl implements CalendarEventService {
 *     
 *     @Override
 *     @Transactional
 *     public CalendarEvent createEvent(CalendarEventCreateRequest request, User owner) {
 *         // 1. Validate business rules
 *         validateDates(request);
 *         
 *         // 2. Build entity
 *         CalendarEvent event = buildEvent(request, owner);
 *         
 *         // 3. Persist
 *         CalendarEvent saved = repository.save(event);
 *         
 *         // 4. Emit domain event
 *         eventPublisher.publish(new EventCreatedEvent(saved));
 *         
 *         return saved;
 *     }
 * }
 * }</pre>
 * 
 * @see CalendarEvent
 * @see CalendarEventCreateRequest
 * @see CalendarEventUpdateRequest
 * @see CalendarEventRepository
 * @author Steby Corp
 * @version 1.0
 * @since 1.0
 */
public interface CalendarEventService {

    // =========================================================================
    // Create Operations
    // =========================================================================

    /**
     * Creates a new calendar event.
     * 
     * <p>This method handles the complete event creation process:</p>
     * <ol>
     *   <li>Validates business rules (dates, required fields, etc.)</li>
     *   <li>Associates event with the owner user</li>
     *   <li>Persists event to database</li>
     *   <li>Emits {@code CalendarEventCreatedEvent} for audit/notifications</li>
     * </ol>
     * 
     * <p><b>Validation Rules:</b></p>
     * <ul>
     *   <li>Title must not be empty</li>
     *   <li>Start date must not be null</li>
     *   <li>End date must not be null</li>
     *   <li>End date must be after or equal to start date</li>
     *   <li>Owner must be a valid, active user</li>
     * </ul>
     * 
     * <p><b>Default Values:</b></p>
     * <ul>
     *   <li>Status: {@code EventStatus.CONFIRMED} if not specified</li>
     *   <li>Recurrence: {@code EventRecurrence.NONE} if not specified</li>
     *   <li>All-day: {@code false} if not specified</li>
     * </ul>
     * 
     * <p><b>Transaction:</b> Runs in a write transaction</p>
     * 
     * <p><b>Domain Event:</b> Emits {@code CalendarEventCreatedEvent} after successful creation</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * CalendarEventCreateRequest request = new CalendarEventCreateRequest(
     *     "Team Meeting",
     *     "Weekly sync-up",
     *     "Conference Room A",
     *     Instant.parse("2025-02-10T14:00:00Z"),
     *     Instant.parse("2025-02-10T15:00:00Z"),
     *     false,
     *     EventStatus.CONFIRMED,
     *     EventRecurrence.WEEKLY,
     *     "#3498DB",
     *     15
     * );
     * 
     * CalendarEvent event = service.createEvent(request, currentUser);
     * System.out.println("Created event: " + event.getPublicId());
     * }</pre>
     * 
     * @param request The event creation request containing all event details
     * @param owner The user who will own this event
     * @return The created and persisted CalendarEvent entity
     * @throws IllegalArgumentException if validation fails (invalid dates, missing required fields)
     * @throws NullPointerException if request or owner is null
     * @see CalendarEventCreateRequest
     * @see be.steby.CoreProject.bll.domains.calendar.events.CalendarEventCreatedEvent
     */
    CalendarEvent createEvent(CalendarEventCreateRequest request, User owner);

    // =========================================================================
    // Read Operations
    // =========================================================================

    /**
     * Retrieves a calendar event by its public ID.
     * 
     * <p>This is the primary method for looking up individual events from API requests.
     * Public IDs are UUIDs and are safe to expose in REST endpoints.</p>
     * 
     * <p><b>Security Note:</b> This method does NOT verify ownership. If you need
     * to ensure the current user owns the event before allowing access, use
     * {@link #verifyOwnership(CalendarEvent, User)} after retrieval.</p>
     * 
     * <p><b>Transaction:</b> Runs in a read-only transaction</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * // Public endpoint: GET /api/calendar/events/{publicId}
     * CalendarEvent event = service.getEventByPublicId(publicId);
     * 
     * // Verify user owns it before allowing modifications
     * service.verifyOwnership(event, currentUser);
     * }</pre>
     * 
     * @param publicId The public UUID identifier of the event
     * @return The CalendarEvent entity
     * @throws CalendarEventNotFoundException if no event found with this public ID
     * @throws IllegalArgumentException if publicId is null or empty
     * @see #verifyOwnership(CalendarEvent, User)
     */
    CalendarEvent getEventByPublicId(String publicId);

    /**
     * Retrieves all events for a specific user, sorted chronologically.
     * 
     * <p>Returns the complete event history for a user including:</p>
     * <ul>
     *   <li>Past events</li>
     *   <li>Current/ongoing events</li>
     *   <li>Future events</li>
     *   <li>All statuses (confirmed, tentative, cancelled)</li>
     * </ul>
     * 
     * <p>Events are sorted by start date ascending (oldest first).</p>
     * 
     * <p><b>Use Cases:</b></p>
     * <ul>
     *   <li>Complete calendar view</li>
     *   <li>Event history/archive page</li>
     *   <li>Export all events to .ics file</li>
     *   <li>User statistics and reports</li>
     * </ul>
     * 
     * <p><b>Performance Note:</b> For large event lists, consider pagination
     * or using date range queries instead.</p>
     * 
     * <p><b>Transaction:</b> Runs in a read-only transaction</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * List<CalendarEvent> allEvents = service.getUserEvents(user.getPublicId());
     * System.out.println("User has " + allEvents.size() + " total events");
     * }</pre>
     * 
     * @param ownerPublicId The public ID of the user whose events to retrieve
     * @return List of CalendarEvent entities sorted by start date (empty list if none)
     * @throws IllegalArgumentException if ownerPublicId is null or empty
     */
    List<CalendarEvent> getUserEvents(String ownerPublicId);

    /**
     * Retrieves events for a user within a specific date range.
     * 
     * <p>Returns events where BOTH start and end times fall within the specified
     * date range. This is the primary query for calendar view components.</p>
     * 
     * <p><b>Date Range Behavior:</b></p>
     * <ul>
     *   <li>Event.startDateTime >= startDate</li>
     *   <li>Event.endDateTime <= endDate</li>
     *   <li>Results sorted by start date ascending</li>
     * </ul>
     * 
     * <p><b>Use Cases:</b></p>
     * <ul>
     *   <li>Calendar month view (e.g., all of February)</li>
     *   <li>Calendar week view (e.g., Feb 5-11)</li>
     *   <li>Calendar day view (e.g., Feb 10)</li>
     *   <li>Custom date range reports</li>
     * </ul>
     * 
     * <p><b>Includes:</b> Events of all statuses (CANCELLED, TENTATIVE, CONFIRMED)</p>
     * 
     * <p><b>Transaction:</b> Runs in a read-only transaction</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * // Get all events in February 2025
     * Instant start = Instant.parse("2025-02-01T00:00:00Z");
     * Instant end = Instant.parse("2025-02-28T23:59:59Z");
     * 
     * List<CalendarEvent> februaryEvents = service.getUserEventsInRange(
     *     user.getPublicId(),
     *     start,
     *     end
     * );
     * }</pre>
     * 
     * @param ownerPublicId The public ID of the user
     * @param startDate Start of the date range (inclusive)
     * @param endDate End of the date range (inclusive)
     * @return List of events within the range, sorted by start date
     * @throws IllegalArgumentException if any parameter is null
     * @throws IllegalArgumentException if endDate is before startDate
     */
    List<CalendarEvent> getUserEventsInRange(String ownerPublicId, Instant startDate, Instant endDate);

    /**
     * Retrieves upcoming events for a user (future non-cancelled events only).
     * 
     * <p>Returns events that:</p>
     * <ul>
     *   <li>Start at or after the current moment</li>
     *   <li>Have status CONFIRMED or TENTATIVE (excludes CANCELLED)</li>
     *   <li>Sorted by start date ascending (soonest first)</li>
     * </ul>
     * 
     * <p>This is one of the most commonly used queries for displaying
     * active upcoming events to users.</p>
     * 
     * <p><b>Use Cases:</b></p>
     * <ul>
     *   <li>"What's next" dashboard widgets</li>
     *   <li>Upcoming events list</li>
     *   <li>Reminder notification system</li>
     *   <li>Calendar preview/summary</li>
     * </ul>
     * 
     * <p><b>Transaction:</b> Runs in a read-only transaction</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * List<CalendarEvent> upcoming = service.getUpcomingEvents(user.getPublicId());
     * 
     * if (!upcoming.isEmpty()) {
     *     CalendarEvent next = upcoming.get(0);
     *     System.out.println("Next event: " + next.getTitle() + 
     *                        " at " + next.getStartDateTime());
     * }
     * }</pre>
     * 
     * @param ownerPublicId The public ID of the user
     * @return List of upcoming non-cancelled events, sorted by start date
     * @throws IllegalArgumentException if ownerPublicId is null or empty
     * @see be.steby.CoreProject.dl.enums.EventStatus#CANCELLED
     */
    List<CalendarEvent> getUpcomingEvents(String ownerPublicId);

    /**
     * Retrieves all events for a user filtered by status.
     *
     * <p>Returns events matching the specified status:</p>
     * <ul>
     *   <li>TENTATIVE: Events pending confirmation</li>
     *   <li>CONFIRMED: Finalized events</li>
     *   <li>CANCELLED: Soft-deleted events (for history/audit)</li>
     * </ul>
     *
     * <p>Events are sorted by start date ascending.</p>
     *
     * <p><b>Use Cases:</b></p>
     * <ul>
     *   <li>Filter calendar view by status</li>
     *   <li>Show only confirmed events</li>
     *   <li>List tentative events needing confirmation</li>
     *   <li>Display cancelled events history</li>
     * </ul>
     *
     * <p><b>Transaction:</b> Runs in a read-only transaction</p>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * // Get only confirmed events
     * List<CalendarEvent> confirmed = service.getUserEventsByStatus(
     *     user.getPublicId(),
     *     EventStatus.CONFIRMED
     * );
     *
     * // Get tentative events for review
     * List<CalendarEvent> tentative = service.getUserEventsByStatus(
     *     user.getPublicId(),
     *     EventStatus.TENTATIVE
     * );
     * }</pre>
     *
     * @param ownerPublicId The public ID of the user whose events to retrieve
     * @param status The event status to filter by
     * @return List of CalendarEvent entities matching the status, sorted by start date
     * @throws IllegalArgumentException if ownerPublicId is null/empty or status is null
     * @see EventStatus
     */
    List<CalendarEvent> getUserEventsByStatus(String ownerPublicId, EventStatus status);

    // =========================================================================
    // Update Operations
    // =========================================================================

    /**
     * Updates an existing calendar event.
     * 
     * <p>This method handles partial updates - only provided fields in the
     * request will be updated. Null fields are ignored and existing values
     * are preserved.</p>
     * 
     * <p><b>Update Process:</b></p>
     * <ol>
     *   <li>Retrieve event by public ID</li>
     *   <li>Verify current user owns the event</li>
     *   <li>Update provided fields</li>
     *   <li>Re-validate business rules (dates, etc.)</li>
     *   <li>Persist changes</li>
     * </ol>
     * 
     * <p><b>Validation Rules:</b></p>
     * <ul>
     *   <li>If both dates provided, endDate must be after startDate</li>
     *   <li>Title cannot be set to empty string</li>
     *   <li>User must own the event</li>
     * </ul>
     * 
     * <p><b>Security:</b> Ownership is verified before any modifications</p>
     * 
     * <p><b>Transaction:</b> Runs in a write transaction</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * // Update only title and location
     * CalendarEventUpdateRequest request = new CalendarEventUpdateRequest(
     *     "Updated Meeting Title",  // title
     *     null,                      // description (unchanged)
     *     "New Conference Room",     // location
     *     null,                      // dates (unchanged)
     *     null,
     *     null,
     *     null,
     *     null,
     *     null,
     *     null
     * );
     * 
     * CalendarEvent updated = service.updateEvent(eventId, request, currentUser);
     * }</pre>
     * 
     * @param publicId The public ID of the event to update
     * @param request The update request with fields to modify (nulls ignored)
     * @param currentUser The user making the update (for ownership verification)
     * @return The updated CalendarEvent entity
     * @throws CalendarEventNotFoundException if event not found
     * @throws OwnershipException if user doesn't own the event
     * @throws IllegalArgumentException if validation fails
     * @see CalendarEventUpdateRequest
     * @see #verifyOwnership(CalendarEvent, User)
     */
    CalendarEvent updateEvent(String publicId, CalendarEventUpdateRequest request, User currentUser);

    /**
     * Cancels an event (soft delete).
     * 
     * <p>Sets the event status to {@link be.steby.CoreProject.dl.enums.EventStatus#CANCELLED}
     * rather than permanently deleting it. This preserves the event for:</p>
     * <ul>
     *   <li>Historical record keeping</li>
     *   <li>Audit trails</li>
     *   <li>Analytics and reporting</li>
     *   <li>Potential rescheduling</li>
     * </ul>
     * 
     * <p><b>Effects of Cancellation:</b></p>
     * <ul>
     *   <li>Event excluded from "upcoming events" queries</li>
     *   <li>No reminders will be sent</li>
     *   <li>Event still visible in "all events" or historical views</li>
     *   <li>Can be displayed with visual indication (strikethrough, grayed out)</li>
     * </ul>
     * 
     * <p><b>Security:</b> Ownership is verified before cancellation</p>
     * 
     * <p><b>Transaction:</b> Runs in a write transaction</p>
     * 
     * <p><b>Domain Event:</b> Emits {@code CalendarEventCancelledEvent} after cancellation</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * CalendarEvent cancelled = service.cancelEvent(eventId, currentUser);
     * System.out.println("Event cancelled: " + cancelled.getTitle());
     * // Event still exists but has status = CANCELLED
     * }</pre>
     * 
     * @param publicId The public ID of the event to cancel
     * @param currentUser The user performing the cancellation
     * @return The cancelled CalendarEvent entity (with status = CANCELLED)
     * @throws CalendarEventNotFoundException if event not found
     * @throws OwnershipException if user doesn't own the event
     * @see be.steby.CoreProject.dl.enums.EventStatus#CANCELLED
     * @see #deleteEvent(String, User) for permanent deletion
     * @see be.steby.CoreProject.bll.domains.calendar.events.CalendarEventCancelledEvent
     */
    CalendarEvent cancelEvent(String publicId, User currentUser);

    // =========================================================================
    // Delete Operations
    // =========================================================================

    /**
     * Permanently deletes an event from the database (hard delete).
     * 
     * <p><b>⚠️ Warning:</b> This is a destructive operation that cannot be undone.
     * The event and all its data will be permanently removed.</p>
     * 
     * <p><b>When to Use:</b></p>
     * <ul>
     *   <li>User explicitly requests permanent deletion</li>
     *   <li>Cleanup of test/spam events</li>
     *   <li>GDPR data deletion requests</li>
     *   <li>System maintenance/cleanup</li>
     * </ul>
     * 
     * <p><b>Recommended Alternative:</b> For most use cases, use
     * {@link #cancelEvent(String, User)} instead to maintain audit trail.</p>
     * 
     * <p><b>Security:</b> Ownership is verified before deletion</p>
     * 
     * <p><b>Transaction:</b> Runs in a write transaction</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * // Confirm with user before hard delete
     * if (userConfirmed) {
     *     service.deleteEvent(eventId, currentUser);
     *     System.out.println("Event permanently deleted");
     * }
     * }</pre>
     * 
     * @param publicId The public ID of the event to delete
     * @param currentUser The user performing the deletion
     * @throws CalendarEventNotFoundException if event not found
     * @throws OwnershipException if user doesn't own the event
     * @see #cancelEvent(String, User) for soft delete (recommended)
     */
    void deleteEvent(String publicId, User currentUser);

    // =========================================================================
    // Security & Validation
    // =========================================================================

    /**
     * Verifies that a user owns a specific event.
     * 
     * <p>This is a critical security method that should be called before
     * any operation that modifies or accesses sensitive event data.</p>
     * 
     * <p><b>Ownership Rule:</b> Event.ownerPublicId must match User.publicId</p>
     * 
     * <p><b>Use Cases:</b></p>
     * <ul>
     *   <li>Before updating event details</li>
     *   <li>Before cancelling or deleting</li>
     *   <li>Before exporting event data</li>
     *   <li>Any operation requiring ownership proof</li>
     * </ul>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * // In controller or service method
     * CalendarEvent event = service.getEventByPublicId(eventId);
     * 
     * // Verify ownership before allowing access
     * service.verifyOwnership(event, currentUser);
     * 
     * // Safe to proceed with operation
     * event.setTitle(newTitle);
     * }</pre>
     * 
     * @param event The event to check ownership for
     * @param user The user claiming ownership
     * @throws OwnershipException if user doesn't own the event
     * @throws NullPointerException if event or user is null
     */
    void verifyOwnership(CalendarEvent event, User user);

    // =========================================================================
    // CRM Integration (internal — no ownership check)
    // =========================================================================

    /**
     * Finds the calendar event linked to a CRM source entity.
     *
     * @param sourcePublicId the public UUID of the CRM source entity
     * @return the linked event, or empty if none exists
     */
    java.util.Optional<CalendarEvent> findBySourcePublicId(String sourcePublicId);

    /**
     * Cancels the calendar event linked to a CRM source entity.
     *
     * <p>No ownership check — called internally by CRM domain listeners.
     * Does nothing if no event is linked to this source.</p>
     *
     * @param sourcePublicId the public UUID of the CRM source entity
     */
    void cancelBySourcePublicId(String sourcePublicId);

    /**
     * Updates the calendar event linked to a CRM source entity.
     *
     * <p>No ownership check — called internally by CRM domain listeners.
     * Also updates {@code ownerPublicId} to reflect potential reassignment.
     * Does nothing if no event is linked to this source.</p>
     *
     * @param sourcePublicId  the public UUID of the CRM source entity
     * @param title           new title (ignored if null)
     * @param description     new description (may be null to clear)
     * @param location        new location (may be null to clear)
     * @param address         new physical address (may be null to clear)
     * @param startDateTime   new start time (ignored if null)
     * @param endDateTime     new end time (ignored if null)
     * @param ownerPublicId   new owner public ID — updated on reassignment (ignored if null)
     */
    void updateBySourcePublicId(String sourcePublicId,
                                String title,
                                String description,
                                String location,
                                be.steby.CoreProject.dl.entities.Address address,
                                java.time.Instant startDateTime,
                                java.time.Instant endDateTime,
                                String ownerPublicId,
                                Integer reminderMinutes);
}