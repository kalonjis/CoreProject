package be.steby.CoreProject.pl.domains.dashboard.controllers;

import be.steby.CoreProject.bll.domains.dashboard.services.CrmDashboardService;
import be.steby.CoreProject.pl.domains.dashboard.models.responses.CrmStatsResponse;
import be.steby.CoreProject.pl.domains.dashboard.models.responses.RevenueMonthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing aggregated CRM dashboard statistics.
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/stats</pre>
 */
@RestController
@RequestMapping("/api/crm/stats")
@RequiredArgsConstructor
@Validated
@Tag(name = "CRM - Dashboard", description = "Aggregated CRM statistics for the dashboard")
public class CrmDashboardController {

    private final CrmDashboardService dashboardService;

    /**
     * Returns a snapshot of key CRM metrics.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/stats</p>
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "Get CRM stats", description = "Returns aggregated metrics for the CRM dashboard")
    public ResponseEntity<CrmStatsResponse> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    /**
     * Returns monthly revenue history for the past N calendar months.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/stats/revenue-history?months=12</p>
     *
     * @param months number of months to look back, including the current month (1–24, default 12)
     * @return ordered list of monthly revenue snapshots, oldest first
     */
    @GetMapping("/revenue-history")
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "Get revenue history", description = "Returns monthly revenue and deal count for the past N months")
    public ResponseEntity<List<RevenueMonthResponse>> getRevenueHistory(
            @RequestParam(defaultValue = "12") @Min(1) @Max(24) int months) {
        return ResponseEntity.ok(dashboardService.getRevenueHistory(months));
    }
}
