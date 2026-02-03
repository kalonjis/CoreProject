package be.steby.CoreProject.pl.domains.calendar.models.responses;

/**
 * DTO containing quick-add URLs for calendar applications.
 * 
 * <p>Provides URLs that open Google Calendar or Outlook Web with
 * event details pre-filled, allowing one-click addition.</p>
 * 
 * <h3>Usage in UI</h3>
 * <pre>{@code
 * // Display as clickable links
 * <a href="${response.googleCalendarUrl}">Add to Google Calendar</a>
 * <a href="${response.outlookWebUrl}">Add to Outlook</a>
 * }</pre>
 * 
 * <h3>JSON Example</h3>
 * <pre>{@code
 * {
 *   "googleCalendarUrl": "https://calendar.google.com/calendar/render?...",
 *   "outlookWebUrl": "https://outlook.live.com/calendar/0/deeplink/compose?..."
 * }
 * }</pre>
 * 
 * @param googleCalendarUrl URL to add event to Google Calendar
 * @param outlookWebUrl URL to add event to Outlook Web
 * 
 * @author Steby Corp
 */
public record CalendarUrlsResponse(
    String googleCalendarUrl,
    String outlookWebUrl
) {
}