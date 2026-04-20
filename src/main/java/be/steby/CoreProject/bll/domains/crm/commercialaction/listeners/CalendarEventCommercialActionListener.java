package be.steby.CoreProject.bll.domains.crm.commercialaction.listeners;

import be.steby.CoreProject.bll.domains.calendar.events.CalendarEventUpdatedEvent;
import be.steby.CoreProject.bll.domains.crm.commercialaction.services.CommercialActionService;
import be.steby.CoreProject.dl.entities.CalendarEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Propagates calendar event updates back to the linked {@link be.steby.CoreProject.dl.entities.crm.CommercialAction}.
 *
 * <h3>Direction</h3>
 * <p>This listener handles the <em>CalendarEvent → CommercialAction</em> direction,
 * which is the reverse of {@code CommercialActionCalendarListener}.
 * Together they form a bidirectional sync between the two domains.</p>
 *
 * <h3>Loop prevention</h3>
 * <p>{@link CommercialActionService#syncFromCalendarEvent} updates the action via
 * the repository directly without publishing a {@code CommercialActionUpdatedEvent}.
 * This means the {@code CommercialActionCalendarListener} is never triggered from
 * a calendar-side change, preventing an infinite sync loop.</p>
 *
 * <h3>Guard conditions</h3>
 * <ul>
 *   <li>Only reacts if {@code calendarEvent.sourceType == "COMMERCIAL_ACTION"}</li>
 *   <li>Silently skips events without a {@code sourcePublicId}</li>
 *   <li>Silently skips actions in terminal state (DONE / CANCELLED)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CalendarEventCommercialActionListener {

    private static final String SOURCE_TYPE = "COMMERCIAL_ACTION";

    private final CommercialActionService commercialActionService;

    @EventListener
    @Transactional
    public void onCalendarEventUpdated(CalendarEventUpdatedEvent event) {
        CalendarEvent calendarEvent = event.event();

        if (!SOURCE_TYPE.equals(calendarEvent.getSourceType())) {
            return;
        }

        String actionPublicId = calendarEvent.getSourcePublicId();
        if (actionPublicId == null || actionPublicId.isBlank()) {
            log.warn("CalendarEvent {} has sourceType COMMERCIAL_ACTION but no sourcePublicId",
                    calendarEvent.getPublicId());
            return;
        }

        log.debug("Syncing CommercialAction {} from CalendarEvent update", actionPublicId);
        commercialActionService.syncFromCalendarEvent(actionPublicId, calendarEvent);
    }
}
