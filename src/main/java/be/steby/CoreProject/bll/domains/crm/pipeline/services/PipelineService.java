package be.steby.CoreProject.bll.domains.crm.pipeline.services;

import be.steby.CoreProject.bll.domains.crm.pipeline.exceptions.*;
import be.steby.CoreProject.bll.domains.crm.pipeline.models.PipelineCreateRequest;
import be.steby.CoreProject.bll.domains.crm.pipeline.models.PipelineStatsResult;
import be.steby.CoreProject.bll.domains.crm.pipeline.models.PipelineStepCreateRequest;
import be.steby.CoreProject.bll.domains.crm.pipeline.models.PipelineStepReorderRequest;
import be.steby.CoreProject.bll.domains.crm.pipeline.models.PipelineStepUpdateRequest;
import be.steby.CoreProject.bll.domains.crm.pipeline.models.PipelineUpdateRequest;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Pipeline;
import be.steby.CoreProject.dl.entities.crm.PipelineStep;

import java.util.List;

/**
 * Service for managing {@link Pipeline} and their {@link PipelineStep}.
 *
 * <p>Pipelines are admin-managed entities that define the commercial process
 * for a specific type of sale. Each pipeline is composed of an ordered list
 * of steps that {@link be.steby.CoreProject.dl.entities.crm.Deal} progress through.</p>
 *
 * <h3>Pipeline lifecycle</h3>
 * <pre>
 * create ──► update ──► delete (only if no active deals)
 * </pre>
 *
 * <h3>Default pipeline</h3>
 * <p>Exactly one pipeline may be flagged as default at a time.
 * Setting a new default automatically clears the flag on the previous one.</p>
 *
 * <h3>Step management</h3>
 * <p>Steps are managed independently from their pipeline via dedicated operations.
 * Reordering is always atomic — the full ordered list must be provided.</p>
 */
public interface PipelineService {

    // =========================================================================
    // Lookup — Pipeline
    // =========================================================================

    /**
     * Finds a pipeline by its public UUID.
     *
     * @param publicId the public UUID of the pipeline
     * @return the matching pipeline
     * @throws PipelineNotFoundException if not found
     */
    Pipeline getByPublicId(String publicId);

    /**
     * Returns the default pipeline.
     *
     * <p>Used when creating a new deal without an explicit pipeline selection.</p>
     *
     * @return the default pipeline
     * @throws PipelineNotFoundException
     *         if no default pipeline is configured
     */
    Pipeline getDefault();

    /**
     * Returns all pipelines ordered by {@code displayOrder} ascending.
     *
     * <p>Used to populate the pipeline selector in the deal creation form.</p>
     *
     * @return all pipelines, sorted by display order
     */
    List<Pipeline> findAll();

    // =========================================================================
    // Lookup — PipelineStep
    // =========================================================================

    /**
     * Finds a pipeline step by its public UUID.
     *
     * @param publicId the public UUID of the step
     * @return the matching step
     * @throws PipelineStepNotFoundException if not found
     */
    PipelineStep getStepByPublicId(String publicId);

    /**
     * Returns all steps of a pipeline, ordered by position ascending.
     *
     * @param pipelinePublicId the public UUID of the pipeline
     * @return ordered list of steps
     * @throws PipelineNotFoundException if pipeline not found
     */
    List<PipelineStep> getSteps(String pipelinePublicId);

    // =========================================================================
    // Stats
    // =========================================================================

    /**
     * Computes conversion and velocity statistics for a pipeline.
     *
     * <p>Returns per-stage funnel metrics (deals entered, conversion rate,
     * average days in stage) computed from {@code DealStageHistory}, plus
     * overall pipeline KPIs (win rate, average deal cycle).</p>
     *
     * @param pipelinePublicId the public UUID of the pipeline
     * @return aggregated pipeline statistics
     * @throws PipelineNotFoundException if not found
     */
    PipelineStatsResult getStats(String pipelinePublicId);

    // =========================================================================
    // Pipeline — Create / Update / Delete
    // =========================================================================

    /**
     * Creates a new pipeline.
     *
     * <p>If {@code request.isDefault()} is {@code true}, the service clears
     * the {@code isDefault} flag on all other pipelines before saving.</p>
     *
     * @param request the pipeline creation data
     * @param actor   the admin performing the operation
     * @return the newly created pipeline
     * @throws PipelineNameAlreadyExistsException
     *         if a pipeline with the same name already exists
     */
    Pipeline create(PipelineCreateRequest request, User actor);

    /**
     * Partially updates an existing pipeline.
     *
     * <p>Only non-null fields in {@link PipelineUpdateRequest} are applied.
     * If {@code isDefault} is set to {@code true}, the previous default pipeline
     * loses its flag automatically.</p>
     *
     * @param publicId the public UUID of the pipeline to update
     * @param request  the partial update data
     * @param actor    the admin performing the operation
     * @return the updated pipeline
     * @throws PipelineNotFoundException if not found
     * @throws PipelineNameAlreadyExistsException
     *         if the new name conflicts with another pipeline
     */
    Pipeline update(String publicId, PipelineUpdateRequest request, User actor);

    /**
     * Deletes a pipeline and all its steps.
     *
     * <p>Only allowed if the pipeline has no active (open) deals.
     * The service checks the deal count before proceeding.</p>
     *
     * @param publicId the public UUID of the pipeline to delete
     * @param actor    the admin performing the operation
     * @throws PipelineNotFoundException if not found
     * @throws PipelineHasActiveDealsException
     *         if the pipeline still has open deals
     */
    void delete(String publicId, User actor);

    // =========================================================================
    // PipelineStep — Create / Update / Delete / Reorder
    // =========================================================================

    /**
     * Adds a new step to an existing pipeline.
     *
     * <p>Validates that:</p>
     * <ul>
     *   <li>{@code isWon} and {@code isLost} are not both {@code true}</li>
     *   <li>No Won step already exists if {@code isWon} is {@code true}</li>
     *   <li>No Lost step already exists if {@code isLost} is {@code true}</li>
     * </ul>
     *
     * @param pipelinePublicId the public UUID of the target pipeline
     * @param request          the step creation data
     * @param actor            the admin performing the operation
     * @return the newly created step
     * @throws PipelineNotFoundException if pipeline not found
     * @throws PipelineValidationException
     *         if terminal flag constraints are violated
     */
    PipelineStep addStep(String pipelinePublicId, PipelineStepCreateRequest request, User actor);

    /**
     * Partially updates an existing pipeline step.
     *
     * <p>Only {@code name} and {@code color} are editable via this operation.
     * Terminal flags and position are managed by dedicated operations.</p>
     *
     * @param stepPublicId the public UUID of the step to update
     * @param request      the partial update data
     * @param actor        the admin performing the operation
     * @return the updated step
     * @throws PipelineStepNotFoundException if not found
     */
    PipelineStep updateStep(String stepPublicId, PipelineStepUpdateRequest request, User actor);

    /**
     * Atomically reorders all steps of a pipeline.
     *
     * <p>The full ordered list must be provided. The service verifies that:</p>
     * <ul>
     *   <li>All provided step public IDs belong to the target pipeline</li>
     *   <li>No step of the pipeline is missing from the request</li>
     *   <li>No duplicate positions are present</li>
     * </ul>
     *
     * @param pipelinePublicId the public UUID of the pipeline
     * @param request          the full reorder request
     * @param actor            the admin performing the operation
     * @return the updated list of steps in their new order
     * @throws PipelineNotFoundException if pipeline not found
     * @throws PipelineValidationException
     *         if the request is incomplete or contains duplicate positions
     */
    List<PipelineStep> reorderSteps(String pipelinePublicId, PipelineStepReorderRequest request, User actor);

    /**
     * Removes a step from a pipeline.
     *
     * <p>Only allowed if no active (open) deals are currently sitting on this step.
     * Terminal steps (Won / Lost) can be deleted as long as no deals are blocked.</p>
     *
     * @param stepPublicId the public UUID of the step to delete
     * @param actor        the admin performing the operation
     * @throws PipelineStepNotFoundException if not found
     * @throws PipelineHasActiveDealsException
     *         if one or more deals are currently at this step
     */
    void deleteStep(String stepPublicId, User actor);
}