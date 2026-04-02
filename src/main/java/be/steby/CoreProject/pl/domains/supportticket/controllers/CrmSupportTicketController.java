package be.steby.CoreProject.pl.domains.supportticket.controllers;

import be.steby.CoreProject.bll.domains.supportticket.services.SupportTicketService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.SupportTicket;
import be.steby.CoreProject.pl.domains.supportticket.models.requests.AssignSupportTicketRequest;
import be.steby.CoreProject.pl.domains.supportticket.models.requests.ChangeSupportTicketStatusRequest;
import be.steby.CoreProject.pl.domains.supportticket.models.requests.CreateSupportTicketRequest;
import be.steby.CoreProject.pl.domains.supportticket.models.requests.SupportTicketListFilterRequest;
import be.steby.CoreProject.pl.domains.supportticket.models.requests.UpdateSupportTicketRequest;
import be.steby.CoreProject.pl.domains.supportticket.models.responses.SupportTicketDetailResponse;
import be.steby.CoreProject.pl.domains.supportticket.models.responses.SupportTicketSummaryResponse;
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
 * REST controller for CRM support ticket management.
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/support-tickets</pre>
 *
 * <h3>Security</h3>
 * <p>Requires {@code COMMERCIAL} or {@code ADMIN} authority on all endpoints.</p>
 */
@RestController
@RequestMapping("/api/crm/support-tickets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
@Slf4j
@Tag(name = "CRM - Support Tickets", description = "Support ticket management")
public class CrmSupportTicketController {

    private final SupportTicketService supportTicketService;

    // =========================================================================
    // Lookup
    // =========================================================================

    @GetMapping
    @Operation(summary = "List support tickets", description = "Returns a paginated, filtered list of support tickets")
    public ResponseEntity<Page<SupportTicketSummaryResponse>> findAll(
            @ModelAttribute SupportTicketListFilterRequest filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("CRM support ticket list requested — filter: {}", filter);

        Page<SupportTicketSummaryResponse> page = supportTicketService
                .findAll(filter.toBllModel(), pageable)
                .map(SupportTicketSummaryResponse::fromEntity);

        return ResponseEntity.ok(page);
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Get support ticket", description = "Returns the full detail of a support ticket")
    public ResponseEntity<SupportTicketDetailResponse> getByPublicId(@PathVariable String publicId) {
        log.debug("CRM support ticket detail requested — publicId: {}", publicId);

        SupportTicket ticket = supportTicketService.getByPublicId(publicId);
        return ResponseEntity.ok(SupportTicketDetailResponse.fromEntity(ticket));
    }

    // =========================================================================
    // Creation & update
    // =========================================================================

    @PostMapping
    @Operation(summary = "Create support ticket", description = "Creates a new support ticket for a contact")
    public ResponseEntity<SupportTicketDetailResponse> create(
            @Valid @RequestBody CreateSupportTicketRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Support ticket creation requested — subject: '{}', by: {}",
                request.subject(), actor.getUsername());

        SupportTicket ticket = supportTicketService.create(request.toBllModel(), actor);
        URI location = URI.create("/api/crm/support-tickets/" + ticket.getPublicId());
        return ResponseEntity.created(location).body(SupportTicketDetailResponse.fromEntity(ticket));
    }

    @PatchMapping("/{publicId}")
    @Operation(summary = "Update support ticket", description = "Partially updates a support ticket's content fields")
    public ResponseEntity<SupportTicketDetailResponse> update(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateSupportTicketRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Support ticket update requested — publicId: {}, by: {}", publicId, actor.getUsername());

        SupportTicket ticket = supportTicketService.update(publicId, request.toBllModel(), actor);
        return ResponseEntity.ok(SupportTicketDetailResponse.fromEntity(ticket));
    }

    // =========================================================================
    // Status
    // =========================================================================

    @PatchMapping("/{publicId}/status")
    @Operation(summary = "Change ticket status", description = "Changes a support ticket's lifecycle status")
    public ResponseEntity<SupportTicketDetailResponse> changeStatus(
            @PathVariable String publicId,
            @Valid @RequestBody ChangeSupportTicketStatusRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Support ticket status change requested — publicId: {}, status: {}, by: {}",
                publicId, request.status(), actor.getUsername());

        SupportTicket ticket = supportTicketService.changeStatus(publicId, request.toBllModel(), actor);
        return ResponseEntity.ok(SupportTicketDetailResponse.fromEntity(ticket));
    }

    // =========================================================================
    // Deletion
    // =========================================================================

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete support ticket", description = "Permanently deletes a support ticket (spam removal, data cleanup)")
    public ResponseEntity<Void> delete(
            @PathVariable String publicId,
            @AuthenticationPrincipal User actor) {

        log.info("Support ticket deletion requested — publicId: {}, by: {}", publicId, actor.getUsername());
        supportTicketService.delete(publicId, actor);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Assignment
    // =========================================================================

    @PatchMapping("/{publicId}/assign")
    @Operation(summary = "Assign support ticket", description = "Assigns or unassigns a support ticket to a team member")
    public ResponseEntity<SupportTicketDetailResponse> assign(
            @PathVariable String publicId,
            @RequestBody AssignSupportTicketRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Support ticket assignment requested — publicId: {}, assignee: {}, by: {}",
                publicId, request.assignedToPublicId(), actor.getUsername());

        SupportTicket ticket = supportTicketService.assign(publicId, request.toBllModel(), actor);
        return ResponseEntity.ok(SupportTicketDetailResponse.fromEntity(ticket));
    }
}
