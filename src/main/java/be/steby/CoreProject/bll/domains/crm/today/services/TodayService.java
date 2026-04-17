package be.steby.CoreProject.bll.domains.crm.today.services;

import be.steby.CoreProject.pl.domains.today.models.responses.TodaySummaryResponse;

/**
 * Service contract for the CRM "Today" view.
 *
 * <p>Aggregates time-sensitive CRM data — overdue actions, today's scheduled actions,
 * overdue deals, deals closing soon, and open support tickets — into a single read-only summary.</p>
 */
public interface TodayService {

    /**
     * Builds and returns the today summary for the current server timezone.
     *
     * @return a {@link TodaySummaryResponse} containing all time-sensitive CRM items for today
     */
    TodaySummaryResponse getSummary();
}
