package be.steby.CoreProject.pl.domains.lead.controllers;

import be.steby.CoreProject.bll.domains.lead.models.LeadDetailModel;
import be.steby.CoreProject.bll.domains.lead.services.LeadService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.pl.domains.lead.models.requests.AssignLeadRequest;
import be.steby.CoreProject.pl.domains.lead.models.requests.ConvertLeadRequest;
import be.steby.CoreProject.pl.domains.lead.models.requests.CreateLeadRequest;
import be.steby.CoreProject.pl.domains.lead.models.requests.EnrichLeadRequest;
import be.steby.CoreProject.pl.domains.lead.models.requests.LeadQueueFilterRequest;
import be.steby.CoreProject.pl.domains.lead.models.requests.RejectLeadRequest;
import be.steby.CoreProject.pl.domains.lead.models.responses.LeadDetailResponse;
import be.steby.CoreProject.pl.domains.lead.models.responses.LeadSummaryResponse;
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
 * REST controller for CRM lead management operations.
 *
 * <p>Exposes the full commercial lifecycle of a lead to authenticated
 * users with {@code COMMERCIAL} or {@code ADMIN} authority:</p>
 * <ul>
 *   <li>Queue browsing with filtering and pagination</li>
 *   <li>Lead detail view</li>
 *   <li>Assignment to a commercial</li>
 *   <li>Status transition to {@code IN_REVIEW}</li>
 *   <li>Conversion to a Contact</li>
 *   <li>Rejection with mandatory reason</li>
 * </ul>
 *
 * <p>All business logic is delegated to {@link LeadService}.
 * This controller only handles HTTP concerns.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/leads</pre>
 *
 * <h3>Security</h3>
 * <p>Requires {@code COMMERCIAL} or {@code ADMIN} authority on all endpoints.</p>
 */
@RestController
@RequestMapping("/api/crm/leads")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
@Slf4j
@Tag(name = "CRM - Leads", description = "Lead queue and lifecycle management")
public class CrmLeadController {

    private final LeadService leadService;

    // =========================================================================
    // Queue & lookup
    // =========================================================================

    /**
     * Returns a paginated, filtered list of leads for the admin queue.
     *
     * <p>All filter parameters are optional — omitting them returns all leads.</p>
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/leads</p>
     *
     * @param filter   optional filter criteria as query parameters
     * @param pageable pagination and sorting (default: 20 per page, newest first)
     * @return paginated list of lead summaries
     */
    @GetMapping
    @Operation(summary = "List leads", description = "Returns a paginated, filtered list of leads")
    public ResponseEntity<Page<LeadSummaryResponse>> findAll(
            @ModelAttribute LeadQueueFilterRequest filter,
            @PageableDefault(size = 20, sort = "submittedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("CRM lead queue requested — filter: {}", filter);

        Page<LeadSummaryResponse> page = leadService
                .findAll(filter.toBllModel(), pageable)
                .map(LeadSummaryResponse::fromEntity);

        return ResponseEntity.ok(page);
    }

    /**
     * Returns the full detail of a single lead.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/leads/{publicId}</p>
     *
     * @param publicId the public UUID of the lead
     * @return the lead detail
     */
    @GetMapping("/{publicId}")
    @Operation(summary = "Get lead", description = "Returns the full detail of a lead, including deduplication context")
    public ResponseEntity<LeadDetailResponse> getByPublicId(@PathVariable String publicId) {
        log.debug("CRM lead detail requested — publicId: {}", publicId);

        LeadDetailModel detail = leadService.getDetail(publicId);
        return ResponseEntity.ok(LeadDetailResponse.fromDetail(detail));
    }

    // =========================================================================
    // Manual creation
    // =========================================================================

    /**
     * Creates a lead manually from the CRM (phone call, business card, etc.).
     *
     * <p>Bypasses honeypot and rate-limit guards. Source is always {@code MANUAL}.</p>
     *
     * <p><strong>Endpoint:</strong> POST /api/crm/leads</p>
     *
     * @param request the lead data
     * @param actor   the authenticated commercial creating the lead
     * @return 201 Created with the new lead detail
     */
    @PostMapping
    @Operation(summary = "Create lead manually", description = "Creates a lead directly from the CRM, bypassing public submission guards")
    public ResponseEntity<LeadDetailResponse> createManual(
            @Valid @RequestBody CreateLeadRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Manual lead creation — email: {}, by: {}", request.email(), actor.getUsername());

        Lead lead = leadService.createManual(request.toBllModel(), actor);
        URI location = URI.create("/api/crm/leads/" + lead.getPublicId());
        return ResponseEntity.created(location).body(LeadDetailResponse.fromEntity(lead));
    }

    // =========================================================================
    // Lifecycle transitions
    // =========================================================================

    /**
     * Enriches a lead with contact details provided or completed by the commercial.
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/leads/{publicId}/enrich</p>
     *
     * @param publicId the public UUID of the lead
     * @param request  the enrichment data (all fields optional)
     * @return the updated lead detail
     */
    @PatchMapping("/{publicId}/enrich")
    @Operation(summary = "Enrich lead", description = "Updates contact details on a lead (firstName, lastName, phone, organisationName)")
    public ResponseEntity<LeadDetailResponse> enrich(
            @PathVariable String publicId,
            @Valid @RequestBody EnrichLeadRequest request) {

        log.info("Lead enrichment requested — publicId: {}", publicId);

        Lead lead = leadService.enrich(publicId, request.toBllModel());
        return ResponseEntity.ok(LeadDetailResponse.fromEntity(lead));
    }

    /**
     * Assigns or reassigns a lead to a commercial.
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/leads/{publicId}/assign</p>
     *
     * @param publicId the public UUID of the lead
     * @param request  the assignment request
     * @param actor    the authenticated user performing the action
     * @return the updated lead detail
     */
    @PatchMapping("/{publicId}/assign")
    @Operation(summary = "Assign lead", description = "Assigns or reassigns a lead to a commercial")
    public ResponseEntity<LeadDetailResponse> assign(
            @PathVariable String publicId,
            @Valid @RequestBody AssignLeadRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Lead assignment requested — publicId: {}, by: {}", publicId, actor.getUsername());

        Lead lead = leadService.assign(publicId, request.toBllModel(), actor);
        return ResponseEntity.ok(LeadDetailResponse.fromEntity(lead));
    }

    /**
     * Transitions a lead to {@code IN_REVIEW} status.
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/leads/{publicId}/review</p>
     *
     * @param publicId the public UUID of the lead
     * @param actor    the authenticated user performing the action
     * @return the updated lead detail
     */
    @PatchMapping("/{publicId}/review")
    @Operation(summary = "Mark lead in review", description = "Transitions a lead to IN_REVIEW status")
    public ResponseEntity<LeadDetailResponse> markInReview(
            @PathVariable String publicId,
            @AuthenticationPrincipal User actor) {

        log.info("Lead marked IN_REVIEW — publicId: {}, by: {}", publicId, actor.getUsername());

        Lead lead = leadService.markInReview(publicId, actor);
        return ResponseEntity.ok(LeadDetailResponse.fromEntity(lead));
    }

    /**
     * Converts a lead into a Contact.
     *
     * <p><strong>Endpoint:</strong> POST /api/crm/leads/{publicId}/convert</p>
     *
     * @param publicId the public UUID of the lead
     * @param request  the conversion data used to create the Contact
     * @param actor    the authenticated user performing the action
     * @return the updated lead detail
     */
    @PostMapping("/{publicId}/convert")
    @Operation(summary = "Convert lead", description = "Converts a lead into a Contact")
    public ResponseEntity<LeadDetailResponse> convert(
            @PathVariable String publicId,
            @Valid @RequestBody ConvertLeadRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Lead conversion requested — publicId: {}, by: {}", publicId, actor.getUsername());

        Lead lead = leadService.convert(publicId, request.toBllModel(), actor);
        return ResponseEntity.ok(LeadDetailResponse.fromEntity(lead));
    }

    /**
     * Rejects a lead with a mandatory reason.
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/leads/{publicId}/reject</p>
     *
     * @param publicId the public UUID of the lead
     * @param request  the rejection request containing the reason
     * @param actor    the authenticated user performing the action
     * @return the updated lead detail
     */
    @PatchMapping("/{publicId}/reject")
    @Operation(summary = "Reject lead", description = "Rejects a lead with a mandatory reason")
    public ResponseEntity<LeadDetailResponse> reject(
            @PathVariable String publicId,
            @Valid @RequestBody RejectLeadRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Lead rejection requested — publicId: {}, by: {}", publicId, actor.getUsername());

        Lead lead = leadService.reject(publicId, request.toBllModel(), actor);
        return ResponseEntity.ok(LeadDetailResponse.fromEntity(lead));
    }
}