package be.steby.CoreProject.pl.domains.today.models.responses;

import be.steby.CoreProject.pl.domains.commercialaction.models.responses.CommercialActionResponse;
import be.steby.CoreProject.pl.domains.deal.models.responses.DealSummaryResponse;
import be.steby.CoreProject.pl.domains.supportticket.models.responses.SupportTicketSummaryResponse;

import java.util.List;

/**
 * Aggregated "Today" summary returned by {@code GET /api/crm/today}.
 */
public record TodaySummaryResponse(

        /** Commercial actions whose due date has already passed. */
        List<CommercialActionResponse> overdueActions,

        /** Commercial actions due today (not yet completed). */
        List<CommercialActionResponse> todayActions,

        /** Open deals whose expected close date has already passed. */
        List<DealSummaryResponse> dealsOverdue,

        /** Open deals whose expected close date falls within the next 7 days. */
        List<DealSummaryResponse> dealsClosingSoon,

        /** Support tickets with status OPEN or IN_PROGRESS. */
        List<SupportTicketSummaryResponse> openTickets
) {}
