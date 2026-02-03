package be.steby.CoreProject.bll.domains.calendar.services;

import be.steby.CoreProject.bll.domains.calendar.events.CalendarEventCancelledEvent;
import be.steby.CoreProject.bll.domains.calendar.events.CalendarEventCreatedEvent;
import be.steby.CoreProject.bll.domains.calendar.exceptions.CalendarEventNotFoundException;
import be.steby.CoreProject.bll.domains.calendar.models.CalendarEventCreateRequest;
import be.steby.CoreProject.bll.domains.calendar.models.CalendarEventUpdateRequest;
import be.steby.CoreProject.bll.exceptions.OwnershipException;
import be.steby.CoreProject.dal.CalendarEventRepository;
import be.steby.CoreProject.dl.entities.CalendarEvent;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.EventStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Implementation of {@link CalendarEventService}.
 * 
 * <p>Handles all calendar event business logic with proper transaction management,
 * validation, security checks, and domain event publishing.</p>
 * 
 * <h3>Key Features</h3>
 * <ul>
 *   <li>Automatic ownership verification for all write operations</li>
 *   <li>Domain event publishing for audit trails</li>
 *   <li>Comprehensive business rule validation</li>
 *   <li>Read-only optimization for query operations</li>
 * </ul>
 * 
 * @see CalendarEventService
 * @author Steby Corp
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CalendarEventServiceImpl implements CalendarEventService {

    private final CalendarEventRepository eventRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * {@inheritDoc}
     * 
     * <p>Validates dates, creates entity, persists, and emits creation event.</p>
     */
    @Override
    @Transactional
    public CalendarEvent createEvent(CalendarEventCreateRequest request, User owner) {
        log.debug("Creating calendar event for user: {}", owner.getUsername());

        // Validate business rules
        validateEventDates(request.startDateTime(), request.endDateTime());

        // Build entity
        CalendarEvent event = CalendarEvent.builder()
                .ownerPublicId(owner.getPublicId())
                .title(request.title())
                .description(request.description())
                .location(request.location())
                .startDateTime(request.startDateTime())
                .endDateTime(request.endDateTime())
                .allDay(request.allDay())
                .status(request.status() != null ? request.status() : EventStatus.CONFIRMED)
                .recurrence(request.recurrence())
                .colorCode(request.colorCode())
                .reminderMinutes(request.reminderMinutes())
                .build();

        // Persist
        CalendarEvent savedEvent = eventRepository.save(event);

        // Emit domain event
        eventPublisher.publishEvent(new CalendarEventCreatedEvent(savedEvent, owner));

        log.info("Calendar event created: {} for user: {}", 
                 savedEvent.getPublicId(), owner.getUsername());

        return savedEvent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CalendarEvent getEventByPublicId(String publicId) {
        return eventRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CalendarEventNotFoundException(publicId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CalendarEvent> getUserEvents(String ownerPublicId) {
        log.debug("Retrieving all events for user: {}", ownerPublicId);
        return eventRepository.findByOwnerPublicId(ownerPublicId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CalendarEvent> getUserEventsInRange(String ownerPublicId, Instant startDate, Instant endDate) {
        log.debug("Retrieving events for user {} from {} to {}", 
                  ownerPublicId, startDate, endDate);
        
        validateEventDates(startDate, endDate);
        
        return eventRepository.findByOwnerAndDateRange(ownerPublicId, startDate, endDate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CalendarEvent> getUpcomingEvents(String ownerPublicId) {
        log.debug("Retrieving upcoming events for user: {}", ownerPublicId);
        return eventRepository.findUpcomingEvents(ownerPublicId, Instant.now());
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Supports partial updates - only provided fields are modified.</p>
     */
    @Override
    @Transactional
    public CalendarEvent updateEvent(String publicId, CalendarEventUpdateRequest request, User currentUser) {
        log.debug("Updating event: {}", publicId);

        CalendarEvent event = getEventByPublicId(publicId);
        verifyOwnership(event, currentUser);

        // Update fields if provided
        if (request.title() != null) event.setTitle(request.title());
        if (request.description() != null) event.setDescription(request.description());
        if (request.location() != null) event.setLocation(request.location());
        if (request.startDateTime() != null) event.setStartDateTime(request.startDateTime());
        if (request.endDateTime() != null) event.setEndDateTime(request.endDateTime());
        if (request.allDay() != null) event.setAllDay(request.allDay());
        if (request.status() != null) event.setStatus(request.status());
        if (request.recurrence() != null) event.setRecurrence(request.recurrence());
        if (request.colorCode() != null) event.setColorCode(request.colorCode());
        if (request.reminderMinutes() != null) event.setReminderMinutes(request.reminderMinutes());

        // Re-validate dates if both were provided
        if (request.startDateTime() != null && request.endDateTime() != null) {
            validateEventDates(event.getStartDateTime(), event.getEndDateTime());
        }

        CalendarEvent updatedEvent = eventRepository.save(event);

        log.info("Event updated: {}", publicId);
        return updatedEvent;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Soft delete - sets status to CANCELLED and emits cancellation event.</p>
     */
    @Override
    @Transactional
    public CalendarEvent cancelEvent(String publicId, User currentUser) {
        log.debug("Cancelling event: {}", publicId);

        CalendarEvent event = getEventByPublicId(publicId);
        verifyOwnership(event, currentUser);

        event.setStatus(EventStatus.CANCELLED);
        CalendarEvent cancelledEvent = eventRepository.save(event);

        // Emit domain event
        eventPublisher.publishEvent(new CalendarEventCancelledEvent(cancelledEvent, currentUser));

        log.info("Event cancelled: {}", publicId);
        return cancelledEvent;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Hard delete - permanently removes the event from database.</p>
     */
    @Override
    @Transactional
    public void deleteEvent(String publicId, User currentUser) {
        log.debug("Deleting event: {}", publicId);

        CalendarEvent event = getEventByPublicId(publicId);
        verifyOwnership(event, currentUser);

        eventRepository.delete(event);

        log.info("Event deleted: {}", publicId);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Throws OwnershipException if ownership doesn't match.</p>
     */
    @Override
    public void verifyOwnership(CalendarEvent event, User user) {
        if (!event.getOwnerPublicId().equals(user.getPublicId())) {
            log.warn("User {} attempted to access event owned by {}", 
                     user.getPublicId(), event.getOwnerPublicId());
            throw new OwnershipException(
                "You do not have permission to modify this event");
        }
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * Validates that end date is after or equal to start date.
     * 
     * @param startDateTime Event start date/time
     * @param endDateTime Event end date/time
     * @throws IllegalArgumentException if end date is before start date
     */
    private void validateEventDates(Instant startDateTime, Instant endDateTime) {
        if (endDateTime.isBefore(startDateTime)) {
            throw new IllegalArgumentException("End date must be after or equal to start date");
        }
    }
}