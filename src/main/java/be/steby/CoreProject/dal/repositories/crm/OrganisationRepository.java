package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.crm.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link Organisation} entity operations.
 *
 * <p>Extends both {@link JpaRepository} for standard CRUD operations and
 * {@link JpaSpecificationExecutor} for dynamic multi-criteria filtering
 * used in the admin organisation list (filter by name, industry, size, etc.).</p>
 *
 * <h3>Query strategy</h3>
 * <ul>
 *   <li>Simple lookups → derived query methods ({@code findBy...})</li>
 *   <li>Dynamic admin filters → {@code OrganisationSpecification}
 *       via {@code findAll(Specification, Pageable)}</li>
 * </ul>
 *
 * @see be.steby.CoreProject.dal.specifications.crm.OrganisationSpecification
 */
@Repository
public interface OrganisationRepository extends JpaRepository<Organisation, Long>,
        JpaSpecificationExecutor<Organisation> {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds an organisation by its public UUID.
     *
     * @param publicId the public UUID
     * @return the organisation if found
     */
    Optional<Organisation> findByPublicId(String publicId);

    /**
     * Finds an organisation by its exact name (case-insensitive).
     *
     * <p>Used for deduplication checks before creating a new organisation.</p>
     *
     * @param name the organisation name
     * @return the organisation if found
     */
    Optional<Organisation> findByNameIgnoreCase(String name);

    /**
     * Returns {@code true} if an organisation with the given name already exists.
     *
     * <p>Used for fast duplicate detection at the service layer.</p>
     *
     * @param name the organisation name to check (case-insensitive)
     * @return true if a matching organisation exists
     */
    boolean existsByNameIgnoreCase(String name);
}