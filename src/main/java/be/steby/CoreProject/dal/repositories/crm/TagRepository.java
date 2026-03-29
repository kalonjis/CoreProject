package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.crm.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByPublicId(String publicId);

    boolean existsByNameIgnoreCase(String name);

    List<Tag> findAllByOrderByNameAsc();

    @Modifying
    @Query(value = "DELETE FROM contact_tag WHERE tag_id = :tagId", nativeQuery = true)
    void removeFromAllContacts(@Param("tagId") Long tagId);

    @Modifying
    @Query(value = "DELETE FROM deal_tag WHERE tag_id = :tagId", nativeQuery = true)
    void removeFromAllDeals(@Param("tagId") Long tagId);

    @Modifying
    @Query(value = "DELETE FROM organisation_tag WHERE tag_id = :tagId", nativeQuery = true)
    void removeFromAllOrganisations(@Param("tagId") Long tagId);
}
