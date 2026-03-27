package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.crm.Interaction;
import be.steby.CoreProject.dl.enums.crm.InteractionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Interaction} entity operations.
 *
 * <p>Interactions are always accessed in the context of a parent entity
 * (a {@link be.steby.CoreProject.dl.entities.crm.Deal} or a
 * {@link be.steby.CoreProject.dl.entities.crm.Contact}), so targeted
 * derived queries are sufficient — no {@code JpaSpecificationExecutor} needed.</p>
 *
 * <h3>Query strategy</h3>
 * <ul>
 *   <li>Timeline queries → derived query methods ordered by {@code occurredAt}</li>
 *   <li>Team reporting → {@code @Query} aggregates</li>
 * </ul>
 */
@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds an Interaction by its public UUID.
     *
     * @param publicId the public UUID
     * @return the Interaction if found
     */
    Optional<Interaction> findByPublicId(String publicId);

    // =========================================================================
    // Deal timeline
    // =========================================================================

    /**
     * Finds all interactions linked to a deal, ordered by occurrence date descending.
     *
     * <p>Used to populate the deal timeline view — most recent Interaction first.</p>
     *
     * @param dealId the internal ID of the deal
     * @return interactions for that deal, most recent first
     */
    List<Interaction> findByDealIdOrderByOccurredAtDesc(Long dealId);

    /**
     * Finds all interactions of a specific type linked to a deal.
     *
     * <p>Used to filter the deal timeline by type (e.g. show only calls).</p>
     *
     * @param dealId the internal ID of the deal
     * @param type   the Interaction type to filter by
     * @return matching interactions, most recent first
     */
    List<Interaction> findByDealIdAndTypeOrderByOccurredAtDesc(Long dealId, InteractionType type);

    /**
     * Finds all interactions linked to a deal OR to the deal's contact,
     * ordered by occurrence date descending.
     *
     * <p>Used for the unified deal timeline: shows both deal-specific interactions
     * and the contact's pre-deal history (e.g. interactions from lead qualification).</p>
     *
     * @param dealId    the internal ID of the deal
     * @param contactId the internal ID of the deal's primary contact
     * @return merged interactions, most recent first
     */
    /**
     * Finds all interactions linked to a deal OR to any of the given contacts,
     * ordered by occurrence date descending.
     *
     * <p>Used for the unified deal timeline with multi-contact support.</p>
     */
    @Query("SELECT DISTINCT i FROM Interaction i LEFT JOIN i.deal d LEFT JOIN i.contact c WHERE d.id = :dealId OR c.id IN :contactIds ORDER BY i.occurredAt DESC")
    List<Interaction> findByDealOrContacts(@Param("dealId") Long dealId, @Param("contactIds") java.util.Collection<Long> contactIds);

    // =========================================================================
    // Contact timeline
    // =========================================================================

    /**
     * Finds all interactions linked to a contact, ordered by occurrence date descending.
     *
     * <p>Used to populate the contact timeline view — shows Interaction across
     * all deals involving that contact.</p>
     *
     * @param contactId the internal ID of the contact
     * @return interactions for that contact, most recent first
     */
    List<Interaction> findByContactIdOrderByOccurredAtDesc(Long contactId);

    /**
     * Finds all interactions linked to a contact directly OR via a deal where that
     * contact is the primary contact, ordered by occurrence date descending.
     *
     * <p>Used for the unified contact timeline: includes interactions logged on the contact's deals.</p>
     *
     * @param contactId the internal ID of the contact
     * @return interactions for that contact or their deals, most recent first
     */
    @Query("SELECT DISTINCT i FROM Interaction i LEFT JOIN i.contact c LEFT JOIN i.deal d LEFT JOIN d.contactRoles dcr LEFT JOIN dcr.contact dc WHERE c.id = :contactId OR dc.id = :contactId ORDER BY i.occurredAt DESC")
    List<Interaction> findByContactOrContactDeal(@Param("contactId") Long contactId);

    // =========================================================================
    // Lead timeline
    // =========================================================================

    /**
     * Finds all interactions linked to a lead, ordered by occurrence date descending.
     *
     * <p>Used during lead qualification — before conversion to a Contact.</p>
     *
     * @param leadId the internal ID of the lead
     * @return interactions for that lead, most recent first
     */
    List<Interaction> findByLeadIdOrderByOccurredAtDesc(Long leadId);

    // =========================================================================
    // Team reporting
    // =========================================================================

    /**
     * Finds all interactions performed by a specific commercial within a date range.
     *
     * <p>Used for individual performance reporting (number of calls, emails, etc.).</p>
     *
     * @param performedById the internal ID of the commercial
     * @param from          start of the range (inclusive)
     * @param to            end of the range (inclusive)
     * @return Interactions performed by that commercial in the range
     */
    List<Interaction> findByPerformedByIdAndOccurredAtBetween(
            Long performedById,
            Instant from,
            Instant to
    );

    /**
     * Counts interactions by type for a specific commercial within a date range.
     *
     * <p>Used for the team Interaction summary dashboard
     * (e.g. "Pierre: 12 calls, 8 emails this week").</p>
     *
     * @param performedById the internal ID of the commercial
     * @param from          start of the range (inclusive)
     * @param to            end of the range (inclusive)
     * @return list of Object[] pairs: [type (ActivityType), count (Long)]
     */
    @Query("SELECT a.type, COUNT(a) FROM Interaction a " +
           "WHERE a.performedBy.id = :performedById " +
           "AND a.occurredAt >= :from AND a.occurredAt <= :to " +
           "GROUP BY a.type")
    List<Object[]> countByTypeForCommercial(
            @Param("performedById") Long performedById,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}