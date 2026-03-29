package be.steby.CoreProject.pl.domains.changelog.controllers;

import be.steby.CoreProject.bll.common.models.changelog.CrmChangeLogFilter;
import be.steby.CoreProject.bll.common.services.changelog.CrmChangeLogQueryService;
import be.steby.CoreProject.dl.entities.crm.CrmChangeLog;
import be.steby.CoreProject.dl.enums.crm.CrmEntityType;
import be.steby.CoreProject.pl.domains.changelog.models.requests.CrmChangeLogFilterRequest;
import be.steby.CoreProject.pl.domains.changelog.models.responses.CrmChangeLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST controller for reading CRM field change log entries.
 *
 * <p>Exposes a single read-only endpoint that returns the audit trail of
 * field changes for any CRM entity (contact, deal, lead, organisation).
 * Write access to the change log is internal — entries are produced
 * automatically by domain services and are never created via the API.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/changelog</pre>
 *
 * @see CrmChangeLogQueryService
 */
@RestController
@RequestMapping("/api/crm/changelog")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "CRM Change Log", description = "Read-only audit trail of CRM field changes")
public class CrmChangeLogController {

    private final CrmChangeLogQueryService crmChangeLogQueryService;

    /**
     * Returns a paginated list of field change entries for a specific CRM entity.
     *
     * <p>Results are sorted by {@code changedAt} descending by default (most recent first).
     * An optional date range can be supplied via {@code from} and {@code to} query parameters.</p>
     *
     * @param entityType  the CRM entity type ({@code CONTACT}, {@code DEAL}, {@code LEAD}, {@code ORGANISATION})
     * @param publicId    the public identifier of the target entity
     * @param from        lower bound on {@code changedAt} (inclusive), ISO-8601; null means no lower bound
     * @param to          upper bound on {@code changedAt} (inclusive), ISO-8601; null means no upper bound
     * @param pageable    pagination and sort parameters; defaults to page 0, size 20, sorted by changedAt DESC
     * @return paginated page of {@link CrmChangeLogResponse}
     */
    /**
     * Returns the most recent field change entries across all CRM entities.
     *
     * <p>No entity filter is applied — this endpoint is intended for global views
     * such as a dashboard widget or an audit log page. Results are always sorted
     * by {@code changedAt} descending.</p>
     *
     * @param pageable pagination parameters; defaults to page 0, size 10, sorted by changedAt DESC
     * @return paginated page of {@link CrmChangeLogResponse}
     */
    @GetMapping("/recent")
    @Operation(summary = "Get the most recent field changes across all CRM entities")
    public ResponseEntity<Page<CrmChangeLogResponse>> getRecent(
            @PageableDefault(size = 10, sort = "changedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("Fetching recent change log — page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<CrmChangeLog> page = crmChangeLogQueryService.getRecent(pageable);
        return ResponseEntity.ok(page.map(CrmChangeLogResponse::from));
    }

    @GetMapping("/{entityType}/{publicId}")
    @Operation(summary = "Get field change history for a CRM entity")
    public ResponseEntity<Page<CrmChangeLogResponse>> getChangeLog(
            @PathVariable CrmEntityType entityType,
            @PathVariable String publicId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "changedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("Fetching change log — entityType: {}, publicId: {}", entityType, publicId);

        CrmChangeLogFilter filter = new CrmChangeLogFilterRequest(entityType, publicId, from, to).toFilter();
        Page<CrmChangeLog> page = crmChangeLogQueryService.getChanges(filter, pageable);

        return ResponseEntity.ok(page.map(CrmChangeLogResponse::from));
    }
}
