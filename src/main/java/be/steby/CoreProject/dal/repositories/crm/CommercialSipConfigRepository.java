package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.crm.CommercialSipConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link CommercialSipConfig} — per-commercial SIP extension credentials.
 */
@Repository
public interface CommercialSipConfigRepository extends JpaRepository<CommercialSipConfig, Long> {

    Optional<CommercialSipConfig> findByPublicId(String publicId);

    Optional<CommercialSipConfig> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsBySipUsername(String sipUsername);
}
