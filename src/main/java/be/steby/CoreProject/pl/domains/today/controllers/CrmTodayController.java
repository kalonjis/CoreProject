package be.steby.CoreProject.pl.domains.today.controllers;

import be.steby.CoreProject.bll.domains.crm.today.services.TodayService;
import be.steby.CoreProject.pl.domains.today.models.responses.TodaySummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the "Today" CRM summary.
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/today</pre>
 */
@RestController
@RequestMapping("/api/crm/today")
@RequiredArgsConstructor
@Tag(name = "CRM - Today", description = "Daily summary: overdue actions, today's actions, deals and tickets")
public class CrmTodayController {

    private final TodayService todayService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "Get today's CRM summary",
               description = "Returns overdue actions, today's actions, overdue deals, deals closing soon, and open tickets")
    public ResponseEntity<TodaySummaryResponse> getSummary() {
        return ResponseEntity.ok(todayService.getSummary());
    }
}
