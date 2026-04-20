package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.crm.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Tag} entities.
 *
 * <p>Exposes standard CRUD operations inherited from {@link JpaRepository} plus
 * domain-specific queries and native bulk-delete statements used during tag deletion.</p>
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * Finds a tag by its public UUID.
     *
     * @param publicId the public UUID of the tag
     * @return an {@link Optional} containing the tag, or empty if not found
     */
    Optional<Tag> findByPublicId(String publicId);

    /**
     * Checks whether a tag with the given name already exists (case-insensitive).
     *
     * @param name the tag name to check
     * @return {@code true} if a tag with that name exists
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Returns all tags sorted by name ascending.
     *
     * @return an ordered list of all tags
     */
    List<Tag> findAllByOrderByNameAsc();

    /**
     * Removes all contact–tag associations for the given tag.
     * Must be called before deleting the tag to avoid FK constraint violations.
     *
     * @param tagId the internal database ID of the tag
     */
    @Modifying
    @Query(value = "DELETE FROM contact_tag WHERE tag_id = :tagId", nativeQuery = true)
    void removeFromAllContacts(@Param("tagId") Long tagId);

    /**
     * Removes all deal–tag associations for the given tag.
     * Must be called before deleting the tag to avoid FK constraint violations.
     *
     * @param tagId the internal database ID of the tag
     */
    @Modifying
    @Query(value = "DELETE FROM deal_tag WHERE tag_id = :tagId", nativeQuery = true)
    void removeFromAllDeals(@Param("tagId") Long tagId);

    /**
     * Removes all organisation–tag associations for the given tag.
     * Must be called before deleting the tag to avoid FK constraint violations.
     *
     * @param tagId the internal database ID of the tag
     */
    @Modifying
    @Query(value = "DELETE FROM organisation_tag WHERE tag_id = :tagId", nativeQuery = true)
    void removeFromAllOrganisations(@Param("tagId") Long tagId);

    /**
     * Removes all lead–tag associations for the given tag.
     * Must be called before deleting the tag to avoid FK constraint violations.
     *
     * @param tagId the internal database ID of the tag
     */
    @Modifying
    @Query(value = "DELETE FROM lead_tag WHERE tag_id = :tagId", nativeQuery = true)
    void removeFromAllLeads(@Param("tagId") Long tagId);
}
