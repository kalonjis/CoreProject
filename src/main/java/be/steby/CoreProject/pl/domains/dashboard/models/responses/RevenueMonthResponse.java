package be.steby.CoreProject.pl.domains.dashboard.models.responses;

import java.math.BigDecimal;

/**
 * Monthly revenue snapshot returned by {@code GET /api/crm/stats/revenue-history}.
 *
 * @param month    ISO year-month string, e.g. {@code "2025-04"}
 * @param revenue  total revenue from deals won in that month (EUR), {@code 0} if none
 * @param dealsWon number of deals closed as WON in that month
 */
public record RevenueMonthResponse(
        String month,
        BigDecimal revenue,
        long dealsWon
) {}
