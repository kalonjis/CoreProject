package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.crm.DealContactRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link DealContactRole} join-entity operations.
 *
 * <p>All access patterns are deal-scoped or contact-scoped.
 * No specification executor needed — queries are targeted and simple.</p>
 */
@Repository
public interface DealContactRoleRepository extends JpaRepository<DealContactRole, Long> {

    /**
     * Finds all contact roles for a given deal.
     *
     * @param dealId the internal ID of the deal
     * @return all roles for that deal
     */
    List<DealContactRole> findByDealId(Long dealId);

    /**
     * Finds the role linking a specific contact to a specific deal.
     *
     * @param dealId    the internal ID of the deal
     * @param contactId the internal ID of the contact
     * @return the role if found
     */
    Optional<DealContactRole> findByDealIdAndContactId(Long dealId, Long contactId);

    /**
     * Returns {@code true} if a contact is already linked to a deal.
     *
     * @param dealId    the internal ID of the deal
     * @param contactId the internal ID of the contact
     * @return true if the link exists
     */
    boolean existsByDealIdAndContactId(Long dealId, Long contactId);

    /**
     * Finds the primary contact role for a given deal.
     *
     * @param dealId the internal ID of the deal
     * @return the primary role if one is set
     */
    @Query("SELECT dcr FROM DealContactRole dcr WHERE dcr.deal.id = :dealId AND dcr.primary = true")
    Optional<DealContactRole> findPrimaryByDealId(@Param("dealId") Long dealId);
}
