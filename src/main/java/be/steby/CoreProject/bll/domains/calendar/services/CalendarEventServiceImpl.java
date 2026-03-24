package be.steby.CoreProject.bll.domains.calendar.services;

import be.steby.CoreProject.bll.domains.address.services.AddressService;
import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.bll.domains.calendar.events.CalendarEventCancelledEvent;
import be.steby.CoreProject.bll.domains.calendar.events.CalendarEventCreatedEvent;
import be.steby.CoreProject.bll.domains.calendar.events.CalendarEventUpdatedEvent;
import be.steby.CoreProject.bll.domains.calendar.exceptions.CalendarEventNotFoundException;
import be.steby.CoreProject.bll.domains.calendar.models.CalendarEventCreateRequest;
import be.steby.CoreProject.bll.domains.calendar.models.CalendarEventUpdateRequest;
import be.steby.CoreProject.bll.common.exceptions.OwnershipException;
import be.steby.CoreProject.dal.CalendarEventRepository;
import be.steby.CoreProject.dl.entities.Address;
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
import java.util.Optional;

/**
 * Implementation of {@link CalendarEventService}.
 *
 * <p>Handles all calendar event business logic with proper transaction management,
 * validation, security checks, and domain event publishing.</p>
 *
 * <h4>Key Features:</h4>
 * <ul>
 *   <li>Automatic ownership verification for all write operations</li>
 *   <li>Domain event publishing for audit trails</li>
 *   <li>Comprehensive business rule validation</li>
 *   <li>Read-only optimization for query operations</li>
 *   <li>Address deduplication via AddressService integration</li>
 * </ul>
 *
 * <h4>Address Handling:</h4>
 * <p>When creating or updating events with address data:</p>
 * <ol>
 *   <li>AddressService.findOrCreate() checks for duplicates</li>
 *   <li>Existing address reused if found</li>
 *   <li>New address created with async geocoding if not found</li>
 * </ol>
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
    private final AddressService addressService;
    private final ApplicationEventPublisher eventPublisher;

    // =========================================================================
    // Create Operations
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Validates dates, resolves address, creates entity, persists, and emits creation event.</p>
     */
    @Override
    @Transactional
    public CalendarEvent createEvent(CalendarEventCreateRequest request, User owner) {
        log.debug("Creating calendar event for user: {}", owner.getUsername());

        // Validate business rules
        validateEventDates(request.startDateTime(), request.endDateTime());

        // Resolve address if provided
        Address resolvedAddress = resolveAddress(request.address());

        // Build entity
        CalendarEvent event = CalendarEvent.builder()
                .ownerPublicId(owner.getPublicId())
                .title(request.title())
                .description(request.description())
                .location(request.location())
                .address(resolvedAddress)
                .startDateTime(request.startDateTime())
                .endDateTime(request.endDateTime())
                .allDay(request.allDay())
                .status(request.status() != null ? request.status() : EventStatus.CONFIRMED)
                .recurrence(request.recurrence())
                .colorCode(request.colorCode())
                .reminderMinutes(request.reminderMinutes())
                .sourceType(request.sourceType())
                .sourcePublicId(request.sourcePublicId())
                .build();

        // Persist
        CalendarEvent savedEvent = eventRepository.save(event);

        // Emit domain event
        eventPublisher.publishEvent(new CalendarEventCreatedEvent(savedEvent, owner));

        log.info("Calendar event created: {} for user: {} (address: {})",
                savedEvent.getPublicId(),
                owner.getUsername(),
                resolvedAddress != null ? resolvedAddress.getPublicId() : "none");

        return savedEvent;
    }

    // =========================================================================
    // Read Operations
    // =========================================================================

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
     */
    @Override
    public List<CalendarEvent> getUserEventsByStatus(String ownerPublicId, EventStatus status) {
        if (ownerPublicId == null || ownerPublicId.isBlank()) {
            throw new IllegalArgumentException("Owner public ID cannot be null or empty");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        log.debug("Retrieving events for user {} with status {}", ownerPublicId, status);
        return eventRepository.findByOwnerAndStatus(ownerPublicId, status);
    }

    // =========================================================================
    // Update Operations
    // =========================================================================

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
        if (request.address() != null) {
            Address resolvedAddress = resolveAddress(request.address());
            event.setAddress(resolvedAddress);
        }
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

        eventPublisher.publishEvent(new CalendarEventUpdatedEvent(updatedEvent, currentUser));

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
    // CRM Integration
    // =========================================================================

    @Override
    public Optional<CalendarEvent> findBySourcePublicId(String sourcePublicId) {
        return eventRepository.findBySourcePublicId(sourcePublicId);
    }

    @Override
    @Transactional
    public void cancelBySourcePublicId(String sourcePublicId) {
        eventRepository.findBySourcePublicId(sourcePublicId).ifPresent(event -> {
            event.setStatus(EventStatus.CANCELLED);
            eventRepository.save(event);
            log.info("Calendar event cancelled from CRM source: {}", sourcePublicId);
        });
    }

    @Override
    @Transactional
    public void updateBySourcePublicId(String sourcePublicId,
                                       String title,
                                       String description,
                                       String location,
                                       Address address,
                                       Instant startDateTime,
                                       Instant endDateTime,
                                       String ownerPublicId,
                                       Integer reminderMinutes) {
        eventRepository.findBySourcePublicId(sourcePublicId).ifPresent(event -> {
            if (title           != null) event.setTitle(title);
            event.setDescription(description);
            event.setLocation(location);
            event.setAddress(address);
            if (startDateTime   != null) event.setStartDateTime(startDateTime);
            if (endDateTime     != null) event.setEndDateTime(endDateTime);
            if (ownerPublicId   != null) event.setOwnerPublicId(ownerPublicId);
            if (reminderMinutes != null) event.setReminderMinutes(reminderMinutes);
            eventRepository.save(event);
            log.info("Calendar event updated from CRM source: {}", sourcePublicId);
        });
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * Resolves an address by finding an existing duplicate or creating new.
     *
     * <p>Uses AddressService.findOrCreate() which:</p>
     * <ul>
     *   <li>Searches for existing address with same street, city, postal code</li>
     *   <li>Returns existing if found (deduplication)</li>
     *   <li>Creates new and triggers async geocoding if not found</li>
     * </ul>
     *
     * @param address the address to resolve (may be null)
     * @return resolved address entity, or null if input was null
     */
    private Address resolveAddress(Address address) {
        if (address == null) {
            return null;
        }

        log.debug("Resolving address: {} {}, {} {}",
                address.getStreetName(),
                address.getStreetNumber(),
                address.getPostalCode(),
                address.getCity());

        Address resolved = addressService.findOrCreate(address);

        log.debug("Address resolved to: {}", resolved.getPublicId());

        return resolved;
    }

    /**
     * Validates that end date is after or equal to start date.
     *
     * @param startDateTime event start date/time
     * @param endDateTime   event end date/time
     * @throws IllegalArgumentException if end date is before start date
     */
    private void validateEventDates(Instant startDateTime, Instant endDateTime) {
        if (endDateTime.isBefore(startDateTime)) {
            throw new IllegalArgumentException("End date must be after or equal to start date");
        }
    }
}