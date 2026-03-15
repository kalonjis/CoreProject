package be.steby.CoreProject.bll.domains.dashboard.services;

import be.steby.CoreProject.pl.domains.dashboard.models.responses.CrmStatsResponse;

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
}
