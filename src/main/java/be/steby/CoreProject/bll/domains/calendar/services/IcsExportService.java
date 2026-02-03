package be.steby.CoreProject.bll.domains.calendar.services;

import be.steby.CoreProject.dl.entities.CalendarEvent;
import java.util.List;

/**
 * Service for exporting calendar events to standard formats.
 * 
 * <p>Provides export capabilities to:</p>
 * <ul>
 *   <li>iCalendar (.ics) format - RFC 5545 compliant</li>
 *   <li>Google Calendar quick-add URLs</li>
 *   <li>Outlook Web quick-add URLs</li>
 * </ul>
 * 
 * <h3>Supported Formats</h3>
 * <ul>
 *   <li><b>.ics files:</b> Universal format for all calendar applications</li>
 *   <li><b>Google Calendar:</b> Direct browser integration</li>
 *   <li><b>Outlook Web:</b> Direct browser integration</li>
 * </ul>
 * 
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5545">RFC 5545 - iCalendar</a>
 * @author Steby Corp
 */
public interface IcsExportService {

    /**
     * Generates iCalendar (.ics) content for a single event.
     * 
     * <p>Output is RFC 5545 compliant and compatible with all major calendar applications.</p>
     * 
     * @param event The event to export
     * @return .ics file content as String
     */
    String generateIcsFile(CalendarEvent event);

    /**
     * Generates iCalendar (.ics) content for multiple events.
     * 
     * <p>Creates a single .ics file containing all events, useful for batch exports.</p>
     * 
     * @param events The events to export
     * @return .ics file content as String
     */
    String generateIcsFile(List<CalendarEvent> events);

    /**
     * Generates Google Calendar quick-add URL.
     * 
     * <p>Returns a URL that opens Google Calendar with event pre-filled,
     * allowing one-click addition to user's calendar.</p>
     * 
     * @param event The event to convert to URL
     * @return Google Calendar URL
     */
    String generateGoogleCalendarUrl(CalendarEvent event);

    /**
     * Generates Outlook Web quick-add URL.
     * 
     * <p>Returns a URL that opens Outlook Web with event pre-filled,
     * allowing one-click addition to user's calendar.</p>
     * 
     * @param event The event to convert to URL
     * @return Outlook Web URL
     */
    String generateOutlookWebUrl(CalendarEvent event);
}