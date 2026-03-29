package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dal.specifications.crm.LeadSpecification;
import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.dl.enums.crm.LeadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Lead} entity operations.
 *
 * <p>Extends both {@link JpaRepository} for standard CRUD operations and
 * {@link JpaSpecificationExecutor} for dynamic multi-criteria filtering
 * used in the admin lead queue (filter by status, type, assigned commercial,
 * date range, etc.).</p>
 *
 * <h3>Query strategy</h3>
 * <ul>
 *   <li>Simple lookups → derived query methods ({@code findBy...})</li>
 *   <li>Anti-spam rate limiting → {@code @Query} (aggregate counts with time window)</li>
 *   <li>Dynamic admin filters → {@code LeadSpecification} via {@code findAll(Specification, Pageable)}</li>
 * </ul>
 *
 * @see LeadSpecification
 */
@Repository
public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds a lead by its public UUID.
     *
     * @param publicId the public UUID
     * @return the lead if found
     */
    Optional<Lead> findByPublicId(String publicId);

    // =========================================================================
    // CRM queue
    // =========================================================================

    /**
     * Finds all leads with a given status, ordered by submission date descending.
     *
     * <p>Used to populate the admin lead queue for a specific status column
     * (e.g., all NEW leads awaiting review).</p>
     *
     * @param status the lead status to filter by
     * @return leads matching the status, newest first
     */
    List<Lead> findByStatusOrderBySubmittedAtDesc(LeadStatus status);

    /**
     * Finds all leads assigned to a specific commercial, ordered by submission date descending.
     *
     * @param assignedToId the internal ID of the assigned commercial
     * @return leads assigned to that commercial, newest first
     */
    List<Lead> findByAssignedToIdOrderBySubmittedAtDesc(Long assignedToId);

    // =========================================================================
    // Anti-spam rate limiting
    // =========================================================================

    /**
     * Counts leads submitted from a specific IP address within a time window.
     *
     * <p>Used for rate limiting to prevent form spam.</p>
     *
     * @param ipAddress the IP address to check
     * @param since     start of the time window
     * @return number of leads from this IP since the given instant
     */
    @Query("SELECT COUNT(l) FROM Lead l " +
            "WHERE l.ipAddress = :ip AND l.submittedAt >= :since")
    long countByIpAddressSince(@Param("ip") String ipAddress, @Param("since") Instant since);

    /**
     * Counts leads submitted from a specific email address within a time window.
     *
     * <p>Used for rate limiting to prevent form spam.</p>
     *
     * @param email the email address to check (case-insensitive)
     * @param since start of the time window
     * @return number of leads from this email since the given instant
     */
    @Query("SELECT COUNT(l) FROM Lead l " +
            "WHERE LOWER(l.email) = LOWER(:email) AND l.submittedAt >= :since")
    long countByEmailSince(@Param("email") String email, @Param("since") Instant since);

    // =========================================================================
    // Dashboard stats
    // =========================================================================

    long countByStatus(LeadStatus status);

    @Query("SELECT l FROM Lead l WHERE " +
           "LOWER(l.firstName) LIKE :kw OR " +
           "LOWER(l.lastName)  LIKE :kw OR " +
           "LOWER(l.email)     LIKE :kw OR " +
           "LOWER(CONCAT(COALESCE(l.firstName,''), ' ', COALESCE(l.lastName,''))) LIKE :kw")
    List<Lead> searchByKeyword(@Param("kw") String keyword, Pageable pageable);
}