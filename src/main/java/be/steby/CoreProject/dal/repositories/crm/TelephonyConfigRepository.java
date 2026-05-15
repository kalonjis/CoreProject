package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.crm.TelephonyConfig;
import be.steby.CoreProject.dl.enums.crm.CallProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link TelephonyConfig} entity operations.
 *
 * <p>Only one {@code TelephonyConfig} should be active at a time.
 * The service layer enforces this invariant on create and update.</p>
 */
@Repository
public interface TelephonyConfigRepository extends JpaRepository<TelephonyConfig, Long> {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds a telephony configuration by its public UUID.
     *
     * @param publicId the public UUID
     * @return the configuration if found
     */
    Optional<TelephonyConfig> findByPublicId(String publicId);

    /**
     * Finds the currently active telephony configuration.
     *
     * <p>Called at application startup and on each call initiation to select
     * the correct {@code TelephonyAdapter}. Returns empty if no configuration
     * has been set up yet — the service layer falls back to {@link CallProvider#TEL_URI}.</p>
     *
     * @return the active configuration if present
     */
    Optional<TelephonyConfig> findByActiveTrue();

    /**
     * Checks whether an active configuration already exists.
     *
     * <p>Used before saving a new configuration to enforce the single-active-config invariant.</p>
     *
     * @return {@code true} if an active configuration is already stored
     */
    boolean existsByActiveTrue();

    /**
     * Finds the configuration for a specific provider, regardless of active state.
     *
     * <p>Used in admin screens to retrieve a previously saved but deactivated configuration.</p>
     *
     * @param provider the telephony provider
     * @return the configuration for that provider if found
     */
    Optional<TelephonyConfig> findByProvider(CallProvider provider);
}
