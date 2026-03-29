package be.steby.CoreProject.pl.domains.pipeline.controllers;

import be.steby.CoreProject.bll.domains.pipeline.services.PipelineService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Pipeline;
import be.steby.CoreProject.dl.entities.crm.PipelineStep;
import be.steby.CoreProject.pl.domains.pipeline.models.requests.*;
import be.steby.CoreProject.pl.domains.pipeline.models.responses.PipelineResponse;
import be.steby.CoreProject.pl.domains.pipeline.models.responses.PipelineStatsResponse;
import be.steby.CoreProject.pl.domains.pipeline.models.responses.PipelineStepResponse;
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
 * REST controller for CRM pipeline and step management.
 *
 * <p>Read operations are accessible to {@code COMMERCIAL} and {@code ADMIN} users.
 * Write operations (create, update, delete, reorder) are restricted to {@code ADMIN}.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/pipelines</pre>
 *
 * <h3>Step sub-resource</h3>
 * <pre>/api/crm/pipelines/{publicId}/steps</pre>
 */
@RestController
@RequestMapping("/api/crm/pipelines")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "CRM - Pipelines", description = "Pipeline and step configuration")
public class CrmPipelineController {

    private final PipelineService pipelineService;

    // =========================================================================
    // Pipeline — Read
    // =========================================================================

    /**
     * Returns all pipelines ordered by display order.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/pipelines</p>
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "List pipelines", description = "Returns all pipelines ordered by display order")
    public ResponseEntity<List<PipelineResponse>> findAll() {
        List<PipelineResponse> pipelines = pipelineService.findAll().stream()
                .map(PipelineResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(pipelines);
    }

    /**
     * Returns the default pipeline.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/pipelines/default</p>
     */
    @GetMapping("/default")
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "Get default pipeline", description = "Returns the default pipeline for new deals")
    public ResponseEntity<PipelineResponse> getDefault() {
        Pipeline pipeline = pipelineService.getDefault();
        return ResponseEntity.ok(PipelineResponse.fromEntity(pipeline));
    }

    /**
     * Returns the full detail of a pipeline including its ordered steps.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/pipelines/{publicId}</p>
     */
    @GetMapping("/{publicId}")
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "Get pipeline", description = "Returns a pipeline with all its steps")
    public ResponseEntity<PipelineResponse> getByPublicId(@PathVariable String publicId) {
        Pipeline pipeline = pipelineService.getByPublicId(publicId);
        return ResponseEntity.ok(PipelineResponse.fromEntity(pipeline));
    }

    // =========================================================================
    // Pipeline — Write (ADMIN only)
    // =========================================================================

    /**
     * Creates a new pipeline.
     *
     * <p><strong>Endpoint:</strong> POST /api/crm/pipelines</p>
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Create pipeline", description = "Creates a new pipeline")
    public ResponseEntity<PipelineResponse> create(
            @Valid @RequestBody CreatePipelineRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Pipeline creation requested — name: '{}', by: {}", request.name(), actor.getUsername());

        Pipeline pipeline = pipelineService.create(request.toBllModel(), actor);
        URI location = URI.create("/api/crm/pipelines/" + pipeline.getPublicId());
        return ResponseEntity.created(location).body(PipelineResponse.fromEntity(pipeline));
    }

    /**
     * Partially updates an existing pipeline.
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/pipelines/{publicId}</p>
     */
    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Update pipeline", description = "Partially updates an existing pipeline")
    public ResponseEntity<PipelineResponse> update(
            @PathVariable String publicId,
            @Valid @RequestBody UpdatePipelineRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Pipeline update requested — publicId: {}, by: {}", publicId, actor.getUsername());

        Pipeline pipeline = pipelineService.update(publicId, request.toBllModel(), actor);
        return ResponseEntity.ok(PipelineResponse.fromEntity(pipeline));
    }

    /**
     * Deletes a pipeline. Only allowed if no active deals are attached.
     *
     * <p><strong>Endpoint:</strong> DELETE /api/crm/pipelines/{publicId}</p>
     */
    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Delete pipeline", description = "Deletes a pipeline if it has no active deals")
    public ResponseEntity<Void> delete(
            @PathVariable String publicId,
            @AuthenticationPrincipal User actor) {

        log.info("Pipeline deletion requested — publicId: {}, by: {}", publicId, actor.getUsername());

        pipelineService.delete(publicId, actor);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Stats
    // =========================================================================

    /**
     * Returns conversion and velocity statistics for a pipeline.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/pipelines/{publicId}/stats</p>
     */
    @GetMapping("/{publicId}/stats")
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "Pipeline stats", description = "Returns per-stage conversion rates and deal velocity for a pipeline")
    public ResponseEntity<PipelineStatsResponse> getStats(@PathVariable String publicId) {
        return ResponseEntity.ok(PipelineStatsResponse.from(pipelineService.getStats(publicId)));
    }

    // =========================================================================
    // Steps — Read
    // =========================================================================

    /**
     * Returns all steps of a pipeline, ordered by position.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/pipelines/{publicId}/steps</p>
     */
    @GetMapping("/{publicId}/steps")
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "List steps", description = "Returns all steps of a pipeline ordered by position")
    public ResponseEntity<List<PipelineStepResponse>> getSteps(@PathVariable String publicId) {
        List<PipelineStepResponse> steps = pipelineService.getSteps(publicId).stream()
                .map(PipelineStepResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(steps);
    }

    // =========================================================================
    // Steps — Write (ADMIN only)
    // =========================================================================

    /**
     * Adds a new step to a pipeline.
     *
     * <p><strong>Endpoint:</strong> POST /api/crm/pipelines/{publicId}/steps</p>
     */
    @PostMapping("/{publicId}/steps")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Add step", description = "Adds a new step to a pipeline")
    public ResponseEntity<PipelineStepResponse> addStep(
            @PathVariable String publicId,
            @Valid @RequestBody AddPipelineStepRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Pipeline step addition requested — pipeline: {}, step: '{}', by: {}",
                publicId, request.name(), actor.getUsername());

        PipelineStep step = pipelineService.addStep(publicId, request.toBllModel(), actor);
        URI location = URI.create("/api/crm/pipelines/" + publicId + "/steps/" + step.getPublicId());
        return ResponseEntity.created(location).body(PipelineStepResponse.fromEntity(step));
    }

    /**
     * Partially updates an existing step.
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/pipelines/{publicId}/steps/{stepPublicId}</p>
     */
    @PatchMapping("/{publicId}/steps/{stepPublicId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Update step", description = "Partially updates a pipeline step")
    public ResponseEntity<PipelineStepResponse> updateStep(
            @PathVariable String publicId,
            @PathVariable String stepPublicId,
            @Valid @RequestBody UpdatePipelineStepRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Pipeline step update requested — step: {}, by: {}", stepPublicId, actor.getUsername());

        PipelineStep step = pipelineService.updateStep(stepPublicId, request.toBllModel(), actor);
        return ResponseEntity.ok(PipelineStepResponse.fromEntity(step));
    }

    /**
     * Atomically reorders all steps of a pipeline.
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/pipelines/{publicId}/steps/reorder</p>
     */
    @PatchMapping("/{publicId}/steps/reorder")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Reorder steps", description = "Atomically reorders all steps of a pipeline")
    public ResponseEntity<List<PipelineStepResponse>> reorderSteps(
            @PathVariable String publicId,
            @Valid @RequestBody ReorderPipelineStepsRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Pipeline steps reorder requested — pipeline: {}, by: {}", publicId, actor.getUsername());

        List<PipelineStepResponse> steps = pipelineService.reorderSteps(publicId, request.toBllModel(), actor)
                .stream()
                .map(PipelineStepResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(steps);
    }

    /**
     * Deletes a step. Only allowed if no active deals are currently at this step.
     *
     * <p><strong>Endpoint:</strong> DELETE /api/crm/pipelines/{publicId}/steps/{stepPublicId}</p>
     */
    @DeleteMapping("/{publicId}/steps/{stepPublicId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Delete step", description = "Deletes a pipeline step if it has no active deals")
    public ResponseEntity<Void> deleteStep(
            @PathVariable String publicId,
            @PathVariable String stepPublicId,
            @AuthenticationPrincipal User actor) {

        log.info("Pipeline step deletion requested — step: {}, by: {}", stepPublicId, actor.getUsername());

        pipelineService.deleteStep(stepPublicId, actor);
        return ResponseEntity.noContent().build();
    }
}
