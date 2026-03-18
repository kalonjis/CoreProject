package be.steby.CoreProject.pl.domains.commercialaction.controllers;

import be.steby.CoreProject.bll.domains.commercialaction.services.CommercialActionService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CommercialAction;
import be.steby.CoreProject.dl.enums.crm.CommercialActionStatus;
import be.steby.CoreProject.pl.domains.commercialaction.models.requests.CreateCommercialActionRequest;
import be.steby.CoreProject.pl.domains.commercialaction.models.requests.ReassignCommercialActionRequest;
import be.steby.CoreProject.pl.domains.commercialaction.models.requests.UpdateCommercialActionRequest;
import be.steby.CoreProject.pl.domains.commercialaction.models.responses.CommercialActionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST controller for CRM commercial action management operations.
 *
 * <p>Exposes the full lifecycle of a commercial action to authenticated
 * users with {@code COMMERCIAL} or {@code ADMIN} authority:</p>
 * <ul>
 *   <li>Single action detail view</li>
 *   <li>Actions by deal or contact</li>
 *   <li>My actions — filtered by the authenticated user, with optional status filter</li>
 *   <li>Create, partial update</li>
 *   <li>Complete, cancel (status transitions)</li>
 *   <li>Commercial reassignment</li>
 * </ul>
 *
 * <p>All business logic is delegated to {@link CommercialActionService}.
 * This controller only handles HTTP concerns.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/commercial-actions</pre>
 *
 * <h3>Security</h3>
 * <p>Requires {@code COMMERCIAL} or {@code ADMIN} authority on all endpoints.</p>
 */
@RestController
@RequestMapping("/api/crm/commercial-actions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
@Slf4j
@Tag(name = "CRM - Commercial Actions", description = "Commercial action task management and lifecycle")
public class CrmCommercialActionController {

    private final CommercialActionService commercialActionService;

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Returns the full detail of a single commercial action.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/commercial-actions/{publicId}</p>
     *
     * @param publicId the public UUID of the commercial action
     * @return the commercial action detail
     */
    @GetMapping("/{publicId}")
    @Operation(summary = "Get commercial action", description = "Returns the full detail of a commercial action")
    public ResponseEntity<CommercialActionResponse> getByPublicId(@PathVariable String publicId) {
        log.debug("CRM commercial action detail requested — publicId: {}", publicId);

        CommercialAction action = commercialActionService.getByPublicId(publicId);
        return ResponseEntity.ok(CommercialActionResponse.fromEntity(action));
    }

    /**
     * Returns all commercial actions linked to a deal, ordered by due date ascending.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/commercial-actions/deal/{dealPublicId}</p>
     *
     * @param dealPublicId the public UUID of the deal
     * @return list of actions linked to that deal
     */
    @GetMapping("/deal/{dealPublicId}")
    @Operation(summary = "Actions by deal", description = "Returns all commercial actions linked to a deal")
    public ResponseEntity<List<CommercialActionResponse>> findByDeal(@PathVariable String dealPublicId) {
        log.debug("CRM commercial actions by deal requested — dealPublicId: {}", dealPublicId);

        List<CommercialActionResponse> actions = commercialActionService.findByDeal(dealPublicId)
                .stream()
                .map(CommercialActionResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(actions);
    }

    /**
     * Returns all commercial actions linked to a contact, ordered by due date ascending.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/commercial-actions/contact/{contactPublicId}</p>
     *
     * @param contactPublicId the public UUID of the contact
     * @return list of actions linked to that contact
     */
    @GetMapping("/contact/{contactPublicId}")
    @Operation(summary = "Actions by contact", description = "Returns all commercial actions linked to a contact")
    public ResponseEntity<List<CommercialActionResponse>> findByContact(@PathVariable String contactPublicId) {
        log.debug("CRM commercial actions by contact requested — contactPublicId: {}", contactPublicId);

        List<CommercialActionResponse> actions = commercialActionService.findByContact(contactPublicId)
                .stream()
                .map(CommercialActionResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(actions);
    }

    /**
     * Returns all commercial actions linked to a lead, ordered by due date ascending.
     *
     * <p>Used during lead qualification — before conversion to a Contact.</p>
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/commercial-actions/lead/{leadPublicId}</p>
     *
     * @param leadPublicId the public UUID of the lead
     * @return list of actions linked to that lead
     */
    @GetMapping("/lead/{leadPublicId}")
    @Operation(summary = "Actions by lead", description = "Returns all commercial actions linked to a lead")
    public ResponseEntity<List<CommercialActionResponse>> findByLead(@PathVariable String leadPublicId) {
        log.debug("CRM commercial actions by lead requested — leadPublicId: {}", leadPublicId);

        List<CommercialActionResponse> actions = commercialActionService.findByLead(leadPublicId)
                .stream()
                .map(CommercialActionResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(actions);
    }

    /**
     * Returns all commercial actions assigned to the authenticated user.
     *
     * <p>Optional {@code status} query parameter filters by lifecycle status.
     * Defaults to {@code PENDING} if not provided.</p>
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/commercial-actions/my</p>
     *
     * @param status the optional status filter (defaults to {@code PENDING} in the service)
     * @param actor  the authenticated user
     * @return list of actions assigned to the authenticated user
     */
    @GetMapping("/my")
    @Operation(summary = "My commercial actions",
               description = "Returns all commercial actions assigned to the authenticated user")
    public ResponseEntity<List<CommercialActionResponse>> findMyActions(
            @RequestParam(required = false) CommercialActionStatus status,
            @AuthenticationPrincipal User actor) {

        log.debug("CRM my commercial actions requested — by: {}, status: {}", actor.getUsername(), status);

        List<CommercialActionResponse> actions = commercialActionService
                .findByAssignedTo(actor.getPublicId(), status)
                .stream()
                .map(CommercialActionResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(actions);
    }

    // =========================================================================
    // Write
    // =========================================================================

    /**
     * Creates a commercial action, linking it to at least one of a deal or contact.
     *
     * <p><strong>Endpoint:</strong> POST /api/crm/commercial-actions</p>
     *
     * @param request the action creation data
     * @param actor   the authenticated user performing the action
     * @return 201 Created with the new commercial action detail
     */
    @PostMapping
    @Operation(summary = "Create commercial action", description = "Creates a new commercial action")
    public ResponseEntity<CommercialActionResponse> create(
            @Valid @RequestBody CreateCommercialActionRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Commercial action creation requested — title: '{}', by: {}",
                request.title(), actor.getUsername());

        CommercialAction action = commercialActionService.create(request.toBllModel(), actor);
        URI location = URI.create("/api/crm/commercial-actions/" + action.getPublicId());
        return ResponseEntity.created(location).body(CommercialActionResponse.fromEntity(action));
    }

    /**
     * Partially updates an existing commercial action's editable fields.
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/commercial-actions/{publicId}</p>
     *
     * @param publicId the public UUID of the action to update
     * @param request  the partial update request
     * @param actor    the authenticated user performing the action
     * @return the updated commercial action detail
     */
    @PatchMapping("/{publicId}")
    @Operation(summary = "Update commercial action", description = "Partially updates an existing commercial action")
    public ResponseEntity<CommercialActionResponse> update(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateCommercialActionRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Commercial action update requested — publicId: {}, by: {}", publicId, actor.getUsername());

        CommercialAction action = commercialActionService.update(publicId, request.toBllModel(), actor);
        return ResponseEntity.ok(CommercialActionResponse.fromEntity(action));
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Marks a commercial action as {@code DONE} and records the completion timestamp.
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/commercial-actions/{publicId}/complete</p>
     *
     * @param publicId the public UUID of the action to complete
     * @param actor    the authenticated user performing the action
     * @return the updated commercial action detail
     */
    @PatchMapping("/{publicId}/complete")
    @Operation(summary = "Complete commercial action",
               description = "Marks a commercial action as DONE and records the completion timestamp")
    public ResponseEntity<CommercialActionResponse> complete(
            @PathVariable String publicId,
            @AuthenticationPrincipal User actor) {

        log.info("Commercial action completion requested — publicId: {}, by: {}", publicId, actor.getUsername());

        CommercialAction action = commercialActionService.complete(publicId, actor);
        return ResponseEntity.ok(CommercialActionResponse.fromEntity(action));
    }

    /**
     * Marks a commercial action as {@code CANCELLED}.
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/commercial-actions/{publicId}/cancel</p>
     *
     * @param publicId the public UUID of the action to cancel
     * @param actor    the authenticated user performing the action
     * @return the updated commercial action detail
     */
    @PatchMapping("/{publicId}/cancel")
    @Operation(summary = "Cancel commercial action", description = "Marks a commercial action as CANCELLED")
    public ResponseEntity<CommercialActionResponse> cancel(
            @PathVariable String publicId,
            @AuthenticationPrincipal User actor) {

        log.info("Commercial action cancellation requested — publicId: {}, by: {}", publicId, actor.getUsername());

        CommercialAction action = commercialActionService.cancel(publicId, actor);
        return ResponseEntity.ok(CommercialActionResponse.fromEntity(action));
    }

    // =========================================================================
    // Assignment
    // =========================================================================

    /**
     * Reassigns a commercial action to a different commercial.
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/commercial-actions/{publicId}/assign</p>
     *
     * @param publicId the public UUID of the action
     * @param request  the reassignment request containing the new assignee's public UUID
     * @param actor    the authenticated user performing the action
     * @return the updated commercial action detail
     */
    @PatchMapping("/{publicId}/assign")
    @Operation(summary = "Reassign commercial action",
               description = "Reassigns a commercial action to a different commercial")
    public ResponseEntity<CommercialActionResponse> reassign(
            @PathVariable String publicId,
            @RequestBody ReassignCommercialActionRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Commercial action reassignment requested — publicId: {}, assignee: {}, by: {}",
                publicId, request.assignedToPublicId(), actor.getUsername());

        CommercialAction action = commercialActionService.reassign(
                publicId, request.assignedToPublicId(), actor);
        return ResponseEntity.ok(CommercialActionResponse.fromEntity(action));
    }
}
