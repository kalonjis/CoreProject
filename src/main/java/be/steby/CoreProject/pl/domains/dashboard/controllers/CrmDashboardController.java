package be.steby.CoreProject.pl.domains.dashboard.controllers;

import be.steby.CoreProject.bll.domains.dashboard.services.CrmDashboardService;
import be.steby.CoreProject.pl.domains.dashboard.models.responses.CrmStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing aggregated CRM dashboard statistics.
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/stats</pre>
 */
@RestController
@RequestMapping("/api/crm/stats")
@RequiredArgsConstructor
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
}
