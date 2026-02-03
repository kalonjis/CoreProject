package be.steby.CoreProject.bll.domains.calendar.services;

import be.steby.CoreProject.dl.entities.CalendarEvent;
import be.steby.CoreProject.dl.enums.EventRecurrence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Implementation of {@link IcsExportService}.
 * 
 * <p>Generates RFC 5545 compliant iCalendar files and quick-add URLs
 * for Google Calendar and Outlook Web.</p>
 * 
 * <h3>Format Standards</h3>
 * <ul>
 *   <li>iCalendar: RFC 5545</li>
 *   <li>Date format: ISO 8601 (yyyyMMdd'T'HHmmss'Z')</li>
 *   <li>Character encoding: UTF-8</li>
 *   <li>Line endings: CRLF (\r\n)</li>
 * </ul>
 * 
 * @see IcsExportService
 * @author Steby Corp
 */
@Service
@Slf4j
public class IcsExportServiceImpl implements IcsExportService {

    private static final DateTimeFormatter ICS_DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateIcsFile(CalendarEvent event) {
        log.debug("Generating ICS file for event: {}", event.getPublicId());

        StringBuilder ics = new StringBuilder();
        appendCalendarHeader(ics);
        appendEvent(ics, event);
        appendCalendarFooter(ics);

        return ics.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateIcsFile(List<CalendarEvent> events) {
        log.debug("Generating ICS file for {} events", events.size());

        StringBuilder ics = new StringBuilder();
        appendCalendarHeader(ics);
        
        for (CalendarEvent event : events) {
            appendEvent(ics, event);
        }
        
        appendCalendarFooter(ics);
        return ics.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateGoogleCalendarUrl(CalendarEvent event) {
        String baseUrl = "https://calendar.google.com/calendar/render";
        
        StringBuilder url = new StringBuilder(baseUrl);
        url.append("?action=TEMPLATE");
        url.append("&text=").append(urlEncode(event.getTitle()));
        url.append("&dates=").append(formatIcsDate(event.getStartDateTime()))
           .append("/").append(formatIcsDate(event.getEndDateTime()));
        
        if (event.getDescription() != null) {
            url.append("&details=").append(urlEncode(event.getDescription()));
        }
        
        if (event.getLocation() != null) {
            url.append("&location=").append(urlEncode(event.getLocation()));
        }

        return url.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateOutlookWebUrl(CalendarEvent event) {
        String baseUrl = "https://outlook.live.com/calendar/0/deeplink/compose";
        
        StringBuilder url = new StringBuilder(baseUrl);
        url.append("?subject=").append(urlEncode(event.getTitle()));
        url.append("&startdt=").append(formatOutlookDate(event.getStartDateTime()));
        url.append("&enddt=").append(formatOutlookDate(event.getEndDateTime()));
        
        if (event.getDescription() != null) {
            url.append("&body=").append(urlEncode(event.getDescription()));
        }
        
        if (event.getLocation() != null) {
            url.append("&location=").append(urlEncode(event.getLocation()));
        }

        return url.toString();
    }

    // =========================================================================
    // Private Helper Methods - iCalendar Generation
    // =========================================================================

    /**
     * Appends iCalendar header to StringBuilder.
     */
    private void appendCalendarHeader(StringBuilder ics) {
        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PRODID:-//Steby Corp//Calendar//FR\r\n");
        ics.append("CALSCALE:GREGORIAN\r\n");
        ics.append("METHOD:PUBLISH\r\n");
    }

    /**
     * Appends a single event in VEVENT format.
     */
    private void appendEvent(StringBuilder ics, CalendarEvent event) {
        ics.append("BEGIN:VEVENT\r\n");
        ics.append("UID:").append(event.getPublicId()).append("@steby.be\r\n");
        ics.append("DTSTAMP:").append(formatIcsDate(Instant.now())).append("\r\n");
        
        // Handle all-day events vs timed events
        if (event.isAllDay()) {
            ics.append("DTSTART;VALUE=DATE:").append(formatIcsDateOnly(event.getStartDateTime())).append("\r\n");
            ics.append("DTEND;VALUE=DATE:").append(formatIcsDateOnly(event.getEndDateTime())).append("\r\n");
        } else {
            ics.append("DTSTART:").append(formatIcsDate(event.getStartDateTime())).append("\r\n");
            ics.append("DTEND:").append(formatIcsDate(event.getEndDateTime())).append("\r\n");
        }
        
        ics.append("SUMMARY:").append(escapeIcsText(event.getTitle())).append("\r\n");
        
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            ics.append("DESCRIPTION:").append(escapeIcsText(event.getDescription())).append("\r\n");
        }
        
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            ics.append("LOCATION:").append(escapeIcsText(event.getLocation())).append("\r\n");
        }
        
        ics.append("STATUS:").append(event.getStatus().name()).append("\r\n");
        
        // Add recurrence rule if applicable
        if (event.getRecurrence() != null && event.getRecurrence() != EventRecurrence.NONE) {
            ics.append("RRULE:FREQ=").append(event.getRecurrence().getIcsFreq()).append("\r\n");
        }
        
        ics.append("END:VEVENT\r\n");
    }

    /**
     * Appends iCalendar footer to StringBuilder.
     */
    private void appendCalendarFooter(StringBuilder ics) {
        ics.append("END:VCALENDAR\r\n");
    }

    // =========================================================================
    // Private Helper Methods - Date Formatting
    // =========================================================================

    /**
     * Formats date for iCalendar: 20250215T140000Z
     */
    private String formatIcsDate(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).format(ICS_DATE_FORMATTER);
    }

    /**
     * Formats date only for all-day events: 20250215
     */
    private String formatIcsDateOnly(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * Formats date for Outlook Web URLs: 2025-02-15T14:00:00
     */
    private String formatOutlookDate(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // =========================================================================
    // Private Helper Methods - Text Escaping & Encoding
    // =========================================================================

    /**
     * Escapes special characters for iCalendar format.
     * 
     * <p>According to RFC 5545, these characters must be escaped:</p>
     * <ul>
     *   <li>Backslash (\) → \\</li>
     *   <li>Comma (,) → \,</li>
     *   <li>Semicolon (;) → \;</li>
     *   <li>Newline (\n) → \n</li>
     * </ul>
     */
    private String escapeIcsText(String text) {
        if (text == null) return "";
        
        return text.replace("\\", "\\\\")
                   .replace(",", "\\,")
                   .replace(";", "\\;")
                   .replace("\n", "\\n")
                   .replace("\r", "");
    }

    /**
     * URL encodes text for calendar URLs.
     */
    private String urlEncode(String text) {
        if (text == null) return "";
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }
}