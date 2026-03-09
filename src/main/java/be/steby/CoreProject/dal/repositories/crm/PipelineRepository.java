package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.crm.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Pipeline} entity operations.
 *
 * <p>Pipelines are a small, mostly static dataset managed by admins.
 * Standard derived queries are sufficient — no dynamic filtering needed.</p>
 */
@Repository
public interface PipelineRepository extends JpaRepository<Pipeline, Long> {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds a pipeline by its public UUID.
     *
     * @param publicId the public UUID
     * @return the pipeline if found
     */
    Optional<Pipeline> findByPublicId(String publicId);

    /**
     * Finds the default pipeline.
     *
     * <p>Used when creating a new deal without an explicit pipeline selection.
     * At most one pipeline should have {@code isDefault = true} at any time —
     * enforced at the service layer.</p>
     *
     * @return the default pipeline if one exists
     */
    Optional<Pipeline> findByIsDefaultTrue();

    /**
     * Finds all pipelines ordered by display order ascending.
     *
     * <p>Used to populate the pipeline selector in the deal creation form.</p>
     *
     * @return all pipelines, sorted by display order
     */
    List<Pipeline> findAllByOrderByDisplayOrderAsc();
}