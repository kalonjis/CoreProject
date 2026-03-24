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

/**
 * REST controller for CRM interaction management (CRUD).
 *
 * <p>Handles single-interaction operations: creation, retrieval, partial update,
 * and deletion. For the unified activity timeline (interactions + completed
 * commercial actions), see
 * {@link be.steby.CoreProject.pl.domains.timeline.controllers.CrmTimelineController}.</p>
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
@Tag(name = "CRM - Interactions", description = "Interaction logging and management (CRUD)")
public class CrmInteractionController {

    private final InteractionService interactionService;

    /**
     * Returns the full detail of a single interaction.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/interactions/{publicId}</p>
     */
    @GetMapping("/{publicId}")
    @Operation(summary = "Get interaction", description = "Returns the full detail of a single interaction")
    public ResponseEntity<InteractionResponse> getByPublicId(@PathVariable String publicId) {
        log.debug("CRM interaction detail requested — publicId: {}", publicId);

        Interaction interaction = interactionService.getByPublicId(publicId);
        return ResponseEntity.ok(InteractionResponse.fromEntity(interaction));
    }

    /**
     * Logs a new interaction against a deal, contact, or lead.
     *
     * <p>For {@code CALL} interactions, provide {@code callLog} details.
     * For {@code EMAIL} interactions, provide {@code emailLog} details.</p>
     *
     * <p><strong>Endpoint:</strong> POST /api/crm/interactions</p>
     */
    @PostMapping
    @Operation(summary = "Log interaction", description = "Logs a new interaction against a deal, contact, or lead")
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
