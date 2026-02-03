package be.steby.CoreProject.pl.domains.calendar.controller;

import be.steby.CoreProject.bll.domains.calendar.services.CalendarEventService;
import be.steby.CoreProject.bll.domains.calendar.services.IcsExportService;
import be.steby.CoreProject.bll.domains.calendar.exceptions.CalendarEventNotFoundException;
import be.steby.CoreProject.dl.entities.CalendarEvent;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.domains.calendar.models.requests.CreateEventRequest;
import be.steby.CoreProject.pl.domains.calendar.models.requests.UpdateEventRequest;
import be.steby.CoreProject.pl.domains.calendar.models.responses.CalendarEventResponse;
import be.steby.CoreProject.pl.domains.calendar.models.responses.CalendarUrlsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * REST Controller for calendar event operations.
 * 
 * <p>Provides endpoints for CRUD operations, event queries, and export functionality.</p>
 * 
 * <h3>Base Path</h3>
 * {@code /api/calendar}
 * 
 * <h3>Security</h3>
 * All endpoints require authentication. Events are automatically scoped to the
 * authenticated user.
 * 
 * <h3>Response Format</h3>
 * <ul>
 *   <li>Success responses: JSON with DTOs (never expose internal IDs)</li>
 *   <li>Error responses: Standard error format via GlobalExceptionHandler</li>
 * </ul>
 * 
 * @see CalendarEventService
 * @see IcsExportService
 * @author Steby Corp
 */
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@Slf4j
public class CalendarController {

    private final CalendarEventService eventService;
    private final IcsExportService icsExportService;

    /**
     * Creates a new calendar event.
     * 
     * <p><b>POST</b> {@code /api/calendar/events}</p>
     * 
     * @param request Event creation details (validated)
     * @param user Authenticated user (injected by Spring Security)
     * @return 201 Created with event DTO
     */
    @PostMapping("/events")
    public ResponseEntity<CalendarEventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal User user) {
        
        log.debug("Creating calendar event for user: {}", user.getUsername());
        CalendarEvent event = eventService.createEvent(request.toBllModel(), user);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CalendarEventResponse.fromEntity(event));
    }

    /**
     * Retrieves a single event by its public ID.
     * 
     * <p><b>GET</b> {@code /api/calendar/events/{publicId}}</p>
     * 
     * @param publicId Event public ID
     * @return 200 OK with event DTO
     * @throws CalendarEventNotFoundException if event not found (404)
     */
    @GetMapping("/events/{publicId}")
    public ResponseEntity<CalendarEventResponse> getEvent(@PathVariable String publicId) {
        log.debug("Retrieving event: {}", publicId);
        CalendarEvent event = eventService.getEventByPublicId(publicId);
        return ResponseEntity.ok(CalendarEventResponse.fromEntity(event));
    }

    /**
     * Retrieves all events for the authenticated user.
     * 
     * <p><b>GET</b> {@code /api/calendar/events}</p>
     * 
     * @param user Authenticated user
     * @return 200 OK with list of event DTOs
     */
    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventResponse>> getUserEvents(
            @AuthenticationPrincipal User user) {
        
        log.debug("Retrieving all events for user: {}", user.getUsername());
        List<CalendarEvent> events = eventService.getUserEvents(user.getPublicId());
        
        return ResponseEntity.ok(
            events.stream().map(CalendarEventResponse::fromEntity).toList()
        );
    }

    /**
     * Retrieves events within a date range for the authenticated user.
     * 
     * <p><b>GET</b> {@code /api/calendar/events/range?start={start}&end={end}}</p>
     * 
     * <p>Date parameters should be ISO 8601 format (e.g., 2025-02-10T14:00:00Z)</p>
     * 
     * @param start Range start date (inclusive)
     * @param end Range end date (inclusive)
     * @param user Authenticated user
     * @return 200 OK with list of event DTOs
     */
    @GetMapping("/events/range")
    public ResponseEntity<List<CalendarEventResponse>> getUserEventsInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @AuthenticationPrincipal User user) {
        
        log.debug("Retrieving events for user {} from {} to {}", 
                  user.getUsername(), start, end);

        List<CalendarEvent> events = eventService.getUserEventsInRange(
            user.getPublicId(), start, end);
        
        return ResponseEntity.ok(
            events.stream().map(CalendarEventResponse::fromEntity).toList()
        );
    }

    /**
     * Retrieves upcoming events for the authenticated user.
     * 
     * <p><b>GET</b> {@code /api/calendar/events/upcoming}</p>
     * 
     * <p>Returns future non-cancelled events only.</p>
     * 
     * @param user Authenticated user
     * @return 200 OK with list of upcoming event DTOs
     */
    @GetMapping("/events/upcoming")
    public ResponseEntity<List<CalendarEventResponse>> getUpcomingEvents(
            @AuthenticationPrincipal User user) {
        
        log.debug("Retrieving upcoming events for user: {}", user.getUsername());
        List<CalendarEvent> events = eventService.getUpcomingEvents(user.getPublicId());
        
        return ResponseEntity.ok(
            events.stream().map(CalendarEventResponse::fromEntity).toList()
        );
    }

    /**
     * Updates an existing event.
     * 
     * <p><b>PUT</b> {@code /api/calendar/events/{publicId}}</p>
     * 
     * <p>Supports partial updates - only provided fields are modified.</p>
     * 
     * @param publicId Event public ID
     * @param request Update details (validated, optional fields)
     * @param user Authenticated user
     * @return 200 OK with updated event DTO
     * @throws be.steby.CoreProject.bll.exceptions.OwnershipException if user doesn't own event (403)
     */
    @PutMapping("/events/{publicId}")
    public ResponseEntity<CalendarEventResponse> updateEvent(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal User user) {
        
        log.debug("Updating event: {}", publicId);
        CalendarEvent event = eventService.updateEvent(publicId, request.toBllModel(), user);
        
        return ResponseEntity.ok(CalendarEventResponse.fromEntity(event));
    }

    /**
     * Cancels an event (soft delete).
     * 
     * <p><b>POST</b> {@code /api/calendar/events/{publicId}/cancel}</p>
     * 
     * <p>Sets status to CANCELLED, preserving event for history.</p>
     * 
     * @param publicId Event public ID
     * @param user Authenticated user
     * @return 200 OK with cancelled event DTO
     * @throws be.steby.CoreProject.bll.exceptions.OwnershipException if user doesn't own event (403)
     */
    @PostMapping("/events/{publicId}/cancel")
    public ResponseEntity<CalendarEventResponse> cancelEvent(
            @PathVariable String publicId,
            @AuthenticationPrincipal User user) {
        
        log.debug("Cancelling event: {}", publicId);
        CalendarEvent event = eventService.cancelEvent(publicId, user);
        
        return ResponseEntity.ok(CalendarEventResponse.fromEntity(event));
    }

    /**
     * Permanently deletes an event (hard delete).
     * 
     * <p><b>DELETE</b> {@code /api/calendar/events/{publicId}}</p>
     * 
     * <p><b>Warning:</b> This is permanent. For most use cases, prefer
     * {@link #cancelEvent} instead.</p>
     * 
     * @param publicId Event public ID
     * @param user Authenticated user
     * @return 204 No Content
     * @throws be.steby.CoreProject.bll.exceptions.OwnershipException if user doesn't own event (403)
     */
    @DeleteMapping("/events/{publicId}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable String publicId,
            @AuthenticationPrincipal User user) {
        
        log.debug("Deleting event: {}", publicId);
        eventService.deleteEvent(publicId, user);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Exports event as downloadable .ics file.
     * 
     * <p><b>GET</b> {@code /api/calendar/events/{publicId}/export}</p>
     * 
     * <p>Returns RFC 5545 compliant iCalendar file.</p>
     * 
     * @param publicId Event public ID
     * @return 200 OK with .ics file download
     */
    @GetMapping("/events/{publicId}/export")
    public ResponseEntity<byte[]> exportEventAsIcs(@PathVariable String publicId) {
        log.debug("Exporting event as ICS: {}", publicId);

        CalendarEvent event = eventService.getEventByPublicId(publicId);
        String icsContent = icsExportService.generateIcsFile(event);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/calendar"));
        headers.setContentDisposition(
            ContentDisposition.attachment()
                .filename(sanitizeFilename(event.getTitle()) + ".ics")
                .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(icsContent.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates quick-add URLs for Google Calendar and Outlook.
     * 
     * <p><b>GET</b> {@code /api/calendar/events/{publicId}/urls}</p>
     * 
     * <p>Returns URLs that open calendar apps with event pre-filled.</p>
     * 
     * @param publicId Event public ID
     * @return 200 OK with calendar URLs
     */
    @GetMapping("/events/{publicId}/urls")
    public ResponseEntity<CalendarUrlsResponse> getCalendarUrls(@PathVariable String publicId) {
        log.debug("Generating calendar URLs for event: {}", publicId);

        CalendarEvent event = eventService.getEventByPublicId(publicId);

        CalendarUrlsResponse response = new CalendarUrlsResponse(
            icsExportService.generateGoogleCalendarUrl(event),
            icsExportService.generateOutlookWebUrl(event)
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Exports all user events as single .ics file.
     * 
     * <p><b>GET</b> {@code /api/calendar/export-all}</p>
     * 
     * @param user Authenticated user
     * @return 200 OK with .ics file download
     */
    @GetMapping("/export-all")
    public ResponseEntity<byte[]> exportAllEventsAsIcs(@AuthenticationPrincipal User user) {
        log.debug("Exporting all events for user: {}", user.getUsername());

        List<CalendarEvent> events = eventService.getUserEvents(user.getPublicId());
        String icsContent = icsExportService.generateIcsFile(events);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/calendar"));
        headers.setContentDisposition(
            ContentDisposition.attachment()
                .filename("my-events.ics")
                .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(icsContent.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sanitizes filename for safe download.
     * Replaces unsafe characters with underscores.
     */
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}