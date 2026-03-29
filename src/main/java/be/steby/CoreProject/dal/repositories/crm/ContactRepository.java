package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.enums.crm.ContactStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Contact} entity operations.
 *
 * <p>Extends both {@link JpaRepository} for standard CRUD operations and
 * {@link JpaSpecificationExecutor} for dynamic multi-criteria filtering
 * used in the admin contact list (filter by status, organisation,
 * assigned commercial, etc.).</p>
 *
 * <h3>Query strategy</h3>
 * <ul>
 *   <li>Simple lookups → derived query methods ({@code findBy...})</li>
 *   <li>Dynamic admin filters → {@code ContactSpecification}
 *       via {@code findAll(Specification, Pageable)}</li>
 * </ul>
 *
 * @see be.steby.CoreProject.dal.specifications.crm.ContactSpecification
 */
@Repository
public interface ContactRepository extends JpaRepository<Contact, Long>,
        JpaSpecificationExecutor<Contact> {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds a contact by its public UUID.
     *
     * @param publicId the public UUID
     * @return the contact if found
     */
    Optional<Contact> findByPublicId(String publicId);

    /**
     * Finds a contact by their email address (case-insensitive).
     *
     * <p>Used for deduplication checks and lead conversion —
     * a lead email may already exist as a contact.</p>
     *
     * @param email the email address
     * @return the contact if found
     */
    Optional<Contact> findByEmailIgnoreCase(String email);

    /**
     * Returns {@code true} if a contact with the given email already exists.
     *
     * <p>Used for fast duplicate detection before lead conversion.</p>
     *
     * @param email the email address to check
     * @return true if a matching contact exists
     */
    boolean existsByEmailIgnoreCase(String email);

    // =========================================================================
    // Organisation
    // =========================================================================

    /**
     * Finds all contacts belonging to a given organisation.
     *
     * <p>Used to populate the contacts tab in the organisation detail view.</p>
     *
     * @param organisationId the internal ID of the organisation
     * @return list of contacts for that organisation
     */
    List<Contact> findByOrganisationId(Long organisationId);

    // =========================================================================
    // Status
    // =========================================================================

    /**
     * Finds all contacts with a given CRM status.
     *
     * <p>Used for bulk operations and status-based reporting
     * (e.g., all CLIENT contacts for a renewal campaign).</p>
     *
     * @param status the contact status to filter by
     * @return list of contacts with that status
     */
    List<Contact> findByStatus(ContactStatus status);

    // =========================================================================
    // User link
    // =========================================================================

    /**
     * Finds the contact linked to a given platform account.
     *
     * <p>Used by {@code ContactService#linkUser} to prevent a user
     * from being linked to more than one contact.</p>
     *
     * @param linkedUser the platform user account
     * @return the contact linked to that user, if any
     */
    Optional<Contact> findByLinkedUser(User linkedUser);

    // =========================================================================
    // Lead traceability
    // =========================================================================

    /**
     * Finds the contact that was converted from a given lead.
     *
     * <p>Used after lead conversion to retrieve the created contact's publicId
     * so that a Deal can be immediately linked to it.</p>
     *
     * @param leadPublicId the public UUID of the origin lead
     * @return the contact if found
     */
    Optional<Contact> findByOriginLead_PublicId(String leadPublicId);

    @Query("SELECT c FROM Contact c WHERE " +
           "LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE :kw OR " +
           "LOWER(c.firstName) LIKE :kw OR " +
           "LOWER(c.lastName)  LIKE :kw OR " +
           "LOWER(c.email)     LIKE :kw")
    List<Contact> searchByKeyword(@Param("kw") String keyword, Pageable pageable);
}