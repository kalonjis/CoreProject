package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.crm.CrmChangeLog;
import be.steby.CoreProject.dl.enums.crm.CrmEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for {@link CrmChangeLog} entries.
 *
 * <p>Supports dynamic filtering via {@link JpaSpecificationExecutor} and
 * provides GDPR-specific bulk operations for anonymisation and hard deletion.</p>
 */
public interface CrmChangeLogRepository
        extends JpaRepository<CrmChangeLog, Long>, JpaSpecificationExecutor<CrmChangeLog> {

    /**
     * Anonymises all change log entries for a given entity.
     *
     * <p>Sets {@code entity_public_id} to {@code null} and replaces
     * {@code old_value} and {@code new_value} with {@code "[anonymised]"}.
     * Called during GDPR right-to-erasure flows where the audit trail
     * must be preserved but personal data removed.</p>
     *
     * @param entityType      the type of the CRM entity
     * @param entityPublicId  the public identifier of the entity to anonymise
     */
    @Modifying
    @Query("""
            UPDATE CrmChangeLog c
            SET c.entityPublicId = null,
                c.oldValue       = '[anonymised]',
                c.newValue       = '[anonymised]'
            WHERE c.entityType      = :entityType
              AND c.entityPublicId  = :entityPublicId
            """)
    void anonymiseByEntity(
            @Param("entityType")     CrmEntityType entityType,
            @Param("entityPublicId") String entityPublicId);

    /**
     * Hard-deletes all change log entries for a given entity.
     *
     * <p>Used when the GDPR request requires full deletion of all records
     * rather than anonymisation.</p>
     *
     * @param entityType      the type of the CRM entity
     * @param entityPublicId  the public identifier of the entity whose logs to delete
     */
    @Modifying
    @Query("""
            DELETE FROM CrmChangeLog c
            WHERE c.entityType     = :entityType
              AND c.entityPublicId = :entityPublicId
            """)
    void deleteByEntity(
            @Param("entityType")     CrmEntityType entityType,
            @Param("entityPublicId") String entityPublicId);
}
