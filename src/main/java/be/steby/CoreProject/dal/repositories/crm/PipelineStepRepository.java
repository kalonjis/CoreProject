package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.crm.PipelineStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link PipelineStep} entity operations.
 *
 * <p>Stages are always accessed in the context of their parent {@link be.steby.CoreProject.dl.entities.crm.Pipeline}.
 * Standard derived queries ordered by position are sufficient — no dynamic filtering needed.</p>
 */
@Repository
public interface PipelineStepRepository extends JpaRepository<PipelineStep, Long> {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds a pipelineStep by its public UUID.
     *
     * @param publicId the public UUID
     * @return the pipelineStep if found
     */
    Optional<PipelineStep> findByPublicId(String publicId);

    // =========================================================================
    // Pipeline context
    // =========================================================================

    /**
     * Finds all stages belonging to a pipeline, ordered by position ascending.
     *
     * <p>Used to build the Kanban column headers and the pipelineStep selector
     * when moving a deal forward.</p>
     *
     * @param pipelineId the internal ID of the pipeline
     * @return stages for that pipeline, in order
     */
    List<PipelineStep> findByPipelineIdOrderByPositionAsc(Long pipelineId);

    /**
     * Finds the Won terminal pipelineStep of a pipeline.
     *
     * <p>Used by the service layer when closing a deal as won.</p>
     *
     * @param pipelineId the internal ID of the pipeline
     * @return the Won pipelineStep if one exists
     */
    Optional<PipelineStep> findByPipelineIdAndIsWonTrue(Long pipelineId);

    /**
     * Finds the Lost terminal pipelineStep of a pipeline.
     *
     * <p>Used by the service layer when closing a deal as lost.</p>
     *
     * @param pipelineId the internal ID of the pipeline
     * @return the Lost pipelineStep if one exists
     */
    Optional<PipelineStep> findByPipelineIdAndIsLostTrue(Long pipelineId);
}