package be.steby.CoreProject.pl.domains.deal.controllers;

import be.steby.CoreProject.bll.domains.deal.services.DealService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Deal;
import be.steby.CoreProject.pl.domains.deal.models.requests.CreateDealRequest;
import be.steby.CoreProject.pl.domains.deal.models.requests.DealListFilterRequest;
import be.steby.CoreProject.pl.domains.deal.models.requests.MoveDealStageRequest;
import be.steby.CoreProject.pl.domains.deal.models.requests.ReassignDealRequest;
import be.steby.CoreProject.pl.domains.deal.models.requests.UpdateDealRequest;
import be.steby.CoreProject.pl.domains.deal.models.responses.DealDetailResponse;
import be.steby.CoreProject.pl.domains.deal.models.responses.DealSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * REST controller for CRM deal management operations.
 *
 * <p>Exposes the full pipeline lifecycle of a deal to authenticated
 * users with {@code COMMERCIAL} or {@code ADMIN} authority:</p>
 * <ul>
 *   <li>Paginated list with filtering</li>
 *   <li>Deal detail view</li>
 *   <li>Manual creation, partial update</li>
 *   <li>Stage move (with automatic won/lost detection)</li>
 *   <li>Commercial reassignment</li>
 * </ul>
 *
 * <p>All business logic is delegated to {@link DealService}.
 * This controller only handles HTTP concerns.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/deals</pre>
 *
 * <h3>Security</h3>
 * <p>Requires {@code COMMERCIAL} or {@code ADMIN} authority on all endpoints.</p>
 */
@RestController
@RequestMapping("/api/crm/deals")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
@Slf4j
@Tag(name = "CRM - Deals", description = "Deal management and pipeline lifecycle")
public class CrmDealController {

    private final DealService dealService;

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Returns a paginated, filtered list of deals.
     *
     * <p>All filter parameters are optional — omitting them returns all deals.</p>
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/deals</p>
     *
     * @param filter   optional filter criteria as query parameters
     * @param pageable pagination and sorting (default: 20 per page, by createdAt descending)
     * @return paginated list of deal summaries
     */
    @GetMapping
    @Operation(summary = "List deals", description = "Returns a paginated, filtered list of deals")
    public ResponseEntity<Page<DealSummaryResponse>> findAll(
            @ModelAttribute DealListFilterRequest filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("CRM deal list requested — filter: {}", filter);

        Page<DealSummaryResponse> page = dealService
                .findAll(filter.toBllModel(), pageable)
                .map(DealSummaryResponse::fromEntity);

        return ResponseEntity.ok(page);
    }

    /**
     * Returns the full detail of a single deal.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/deals/{publicId}</p>
     *
     * @param publicId the public UUID of the deal
     * @return the deal detail
     */
    @GetMapping("/{publicId}")
    @Operation(summary = "Get deal", description = "Returns the full detail of a deal")
    public ResponseEntity<DealDetailResponse> getByPublicId(@PathVariable String publicId) {
        log.debug("CRM deal detail requested — publicId: {}", publicId);

        Deal deal = dealService.getByPublicId(publicId);
        return ResponseEntity.ok(DealDetailResponse.fromEntity(deal));
    }

    // =========================================================================
    // Creation & update
    // =========================================================================

    /**
     * Creates a deal manually, linking it to a pipeline, contact, and commercial.
     *
     * <p><strong>Endpoint:</strong> POST /api/crm/deals</p>
     *
     * @param request the deal creation data
     * @param actor   the authenticated user performing the action
     * @return 201 Created with the new deal detail
     */
    @PostMapping
    @Operation(summary = "Create deal", description = "Creates a deal manually")
    public ResponseEntity<DealDetailResponse> create(
            @Valid @RequestBody CreateDealRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Deal creation requested — title: '{}', by: {}", request.title(), actor.getUsername());

        Deal deal = dealService.create(request.toBllModel(), actor);
        URI location = URI.create("/api/crm/deals/" + deal.getPublicId());
        return ResponseEntity.created(location).body(DealDetailResponse.fromEntity(deal));
    }

    /**
     * Partially updates an existing deal's editable fields.
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/deals/{publicId}</p>
     *
     * @param publicId the public UUID of the deal to update
     * @param request  the partial update request
     * @param actor    the authenticated user performing the action
     * @return the updated deal detail
     */
    @PatchMapping("/{publicId}")
    @Operation(summary = "Update deal", description = "Partially updates an existing deal")
    public ResponseEntity<DealDetailResponse> update(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateDealRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Deal update requested — publicId: {}, by: {}", publicId, actor.getUsername());

        Deal deal = dealService.update(publicId, request.toBllModel(), actor);
        return ResponseEntity.ok(DealDetailResponse.fromEntity(deal));
    }

    // =========================================================================
    // Stage & lifecycle
    // =========================================================================

    /**
     * Moves a deal to a different pipeline stage.
     *
     * <p>Terminal stages (Won/Lost) automatically close the deal and
     * publish the corresponding domain event.</p>
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/deals/{publicId}/stage</p>
     *
     * @param publicId the public UUID of the deal
     * @param request  the stage move request containing the target stage public UUID
     * @param actor    the authenticated user performing the action
     * @return the updated deal detail
     */
    @PatchMapping("/{publicId}/stage")
    @Operation(summary = "Move deal stage", description = "Moves a deal to a different pipeline stage")
    public ResponseEntity<DealDetailResponse> moveToStage(
            @PathVariable String publicId,
            @Valid @RequestBody MoveDealStageRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Deal stage move requested — publicId: {}, stage: {}, by: {}",
                publicId, request.stagePublicId(), actor.getUsername());

        Deal deal = dealService.moveToStage(publicId, request.stagePublicId(), actor);
        return ResponseEntity.ok(DealDetailResponse.fromEntity(deal));
    }

    // =========================================================================
    // Assignment
    // =========================================================================

    /**
     * Reassigns a deal to a different commercial (or unassigns it).
     *
     * <p>Passing {@code null} as {@code assignedToPublicId} removes the current assignee.</p>
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/deals/{publicId}/assign</p>
     *
     * @param publicId the public UUID of the deal
     * @param request  the reassignment request (assignedToPublicId may be null to unassign)
     * @param actor    the authenticated user performing the action
     * @return the updated deal detail
     */
    @PatchMapping("/{publicId}/assign")
    @Operation(summary = "Reassign deal", description = "Reassigns a deal to a different commercial")
    public ResponseEntity<DealDetailResponse> reassign(
            @PathVariable String publicId,
            @RequestBody ReassignDealRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Deal reassignment requested — publicId: {}, assignee: {}, by: {}",
                publicId, request.assignedToPublicId(), actor.getUsername());

        Deal deal = dealService.reassign(publicId, request.toBllModel(), actor);
        return ResponseEntity.ok(DealDetailResponse.fromEntity(deal));
    }
}
