package be.steby.CoreProject.bll.domains.crm.dashboard.services;

import be.steby.CoreProject.pl.domains.dashboard.models.responses.CrmStatsResponse;
import be.steby.CoreProject.pl.domains.dashboard.models.responses.RevenueMonthResponse;

import java.util.List;

/**
 * Service for computing aggregated CRM dashboard statistics.
 */
public interface CrmDashboardService {

    /**
     * Returns a snapshot of key CRM metrics for the dashboard.
     *
     * @return aggregated stats covering leads, deals, actions, and tickets
     */
    CrmStatsResponse getStats();

    /**
     * Returns monthly revenue history for the past {@code months} calendar months.
     *
     * <p>Each entry covers one complete calendar month, from the oldest to the most
     * recent. The current (incomplete) month is included as the last entry.</p>
     *
     * @param months number of months to look back, including the current month (1–24)
     * @return ordered list of monthly revenue snapshots, oldest first
     */
    List<RevenueMonthResponse> getRevenueHistory(int months);
}
