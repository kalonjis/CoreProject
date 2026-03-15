package be.steby.CoreProject.pl.domains.dashboard.models.responses;

import java.math.BigDecimal;

/**
 * Aggregated CRM statistics returned by {@code GET /api/crm/stats}.
 *
 * <p>All counts are current at the time of the request.
 * Monthly figures cover the current calendar month (from the 1st at midnight).</p>
 */
public record CrmStatsResponse(

        /** Leads submitted and not yet reviewed. */
        long leadsNew,

        /** Leads currently being reviewed by a commercial. */
        long leadsInReview,

        /** Deals currently open (not won or lost). */
        long dealsOpen,

        /** Total value of all open deals (EUR). */
        BigDecimal pipelineValue,

        /** Number of deals won this calendar month. */
        long dealsWonThisMonth,

        /** Total revenue from deals won this calendar month (EUR). */
        BigDecimal revenueWonThisMonth,

        /** Pending commercial actions whose due date has passed. */
        long overdueActions,

        /** Support tickets with status OPEN. */
        long ticketsOpen,

        /** Support tickets with status IN_PROGRESS. */
        long ticketsInProgress
) {}
