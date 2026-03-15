package be.steby.CoreProject.pl.domains.interaction.controllers;

import be.steby.CoreProject.bll.domains.interaction.services.InteractionService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Interaction;
import be.steby.CoreProject.pl.domains.interaction.models.requests.LogInteractionRequest;
import be.steby.CoreProject.pl.domains.interaction.models.requests.UpdateInteractionRequest;
import be.steby.CoreProject.pl.domains.interaction.models.responses.InteractionResponse;
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
 * REST controller for CRM interaction management operations.
 *
 * <p>Exposes the interaction timeline and logging capabilities to authenticated
 * users with {@code COMMERCIAL} or {@code ADMIN} authority:</p>
 * <ul>
 *   <li>Single interaction detail view</li>
 *   <li>Deal timeline — all interactions linked to a deal</li>
 *   <li>Contact timeline — all interactions linked to a contact</li>
 *   <li>Log a new interaction (with optional CallLog or EmailLog)</li>
 *   <li>Partial update of editable fields</li>
 *   <li>Permanent deletion (cascades to CallLog / EmailLog)</li>
 * </ul>
 *
 * <p>All business logic is delegated to {@link InteractionService}.
 * This controller only handles HTTP concerns.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/interactions</pre>
 *
 * <h3>Security</h3>
 * <p>Requires {@code COMMERCIAL} or {@code ADMIN} authority on all endpoints.</p>
 */
@RestController
@RequestMapping("/api/crm/interactions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
@Slf4j
@Tag(name = "CRM - Interactions", description = "Interaction timeline logging and management")
public class CrmInteractionController {

    private final InteractionService interactionService;

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Returns the full detail of a single interaction.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/interactions/{publicId}</p>
     *
     * @param publicId the public UUID of the interaction
     * @return the interaction detail
     */
    @GetMapping("/{publicId}")
    @Operation(summary = "Get interaction", description = "Returns the full detail of a single interaction")
    public ResponseEntity<InteractionResponse> getByPublicId(@PathVariable String publicId) {
        log.debug("CRM interaction detail requested — publicId: {}", publicId);

        Interaction interaction = interactionService.getByPublicId(publicId);
        return ResponseEntity.ok(InteractionResponse.fromEntity(interaction));
    }

    /**
     * Returns the interaction timeline for a deal, ordered most recent first.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/interactions/deal/{dealPublicId}</p>
     *
     * @param dealPublicId the public UUID of the deal
     * @return list of interactions linked to that deal
     */
    @GetMapping("/deal/{dealPublicId}")
    @Operation(summary = "Deal timeline", description = "Returns all interactions linked to a deal, most recent first")
    public ResponseEntity<List<InteractionResponse>> getTimelineByDeal(@PathVariable String dealPublicId) {
        log.debug("CRM deal timeline requested — dealPublicId: {}", dealPublicId);

        List<InteractionResponse> timeline = interactionService.getTimelineByDeal(dealPublicId)
                .stream()
                .map(InteractionResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(timeline);
    }

    /**
     * Returns the interaction timeline for a contact, ordered most recent first.
     *
     * <p>Includes interactions across all deals involving that contact.</p>
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/interactions/contact/{contactPublicId}</p>
     *
     * @param contactPublicId the public UUID of the contact
     * @return list of interactions linked to that contact
     */
    @GetMapping("/contact/{contactPublicId}")
    @Operation(summary = "Contact timeline", description = "Returns all interactions linked to a contact, most recent first")
    public ResponseEntity<List<InteractionResponse>> getTimelineByContact(@PathVariable String contactPublicId) {
        log.debug("CRM contact timeline requested — contactPublicId: {}", contactPublicId);

        List<InteractionResponse> timeline = interactionService.getTimelineByContact(contactPublicId)
                .stream()
                .map(InteractionResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(timeline);
    }

    // =========================================================================
    // Write
    // =========================================================================

    /**
     * Logs a new interaction against a deal or contact.
     *
     * <p>For {@code CALL} interactions, provide {@code callLog} details.
     * For {@code EMAIL} interactions, provide {@code emailLog} details.</p>
     *
     * <p><strong>Endpoint:</strong> POST /api/crm/interactions</p>
     *
     * @param request the interaction creation data
     * @param actor   the authenticated user performing the action
     * @return 201 Created with the new interaction detail
     */
    @PostMapping
    @Operation(summary = "Log interaction", description = "Logs a new interaction against a deal or contact")
    public ResponseEntity<InteractionResponse> create(
            @Valid @RequestBody LogInteractionRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Interaction log requested — type: {}, subject: '{}', by: {}",
                request.type(), request.subject(), actor.getUsername());

        Interaction interaction = interactionService.create(request.toBllModel(), actor);
        URI location = URI.create("/api/crm/interactions/" + interaction.getPublicId());
        return ResponseEntity.created(location).body(InteractionResponse.fromEntity(interaction));
    }

    /**
     * Partially updates an existing interaction's editable fields.
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/interactions/{publicId}</p>
     *
     * @param publicId the public UUID of the interaction to update
     * @param request  the partial update request
     * @param actor    the authenticated user performing the action
     * @return the updated interaction detail
     */
    @PatchMapping("/{publicId}")
    @Operation(summary = "Update interaction", description = "Partially updates an existing interaction")
    public ResponseEntity<InteractionResponse> update(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateInteractionRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Interaction update requested — publicId: {}, by: {}", publicId, actor.getUsername());

        Interaction interaction = interactionService.update(publicId, request.toBllModel(), actor);
        return ResponseEntity.ok(InteractionResponse.fromEntity(interaction));
    }

    /**
     * Permanently deletes an interaction and its associated sub-entities.
     *
     * <p>Cascade deletion automatically removes the linked {@code CallLog}
     * or {@code EmailLog} if present.</p>
     *
     * <p><strong>Endpoint:</strong> DELETE /api/crm/interactions/{publicId}</p>
     *
     * @param publicId the public UUID of the interaction to delete
     * @param actor    the authenticated user performing the action
     * @return 204 No Content
     */
    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete interaction", description = "Permanently deletes an interaction and its sub-entities")
    public ResponseEntity<Void> delete(
            @PathVariable String publicId,
            @AuthenticationPrincipal User actor) {

        log.info("Interaction deletion requested — publicId: {}, by: {}", publicId, actor.getUsername());

        interactionService.delete(publicId, actor);
        return ResponseEntity.noContent().build();
    }
}
