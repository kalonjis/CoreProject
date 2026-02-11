package be.steby.CoreProject.bll.domains.address.services.geocoding.fallback;

import be.steby.CoreProject.dal.repositories.FailedGeocodingRepository;
import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.FailedGeocodingEntity;
import be.steby.CoreProject.dl.enums.FailedGeocodingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for failed geocoding attempts.
 * 
 * Responsibilities:
 * - Store failed geocoding attempts in database for later retry
 * - Avoid duplicate records for the same address
 * - Provide visibility into geocoding failures
 * 
 * Called by NominatimProvider's fallback method when:
 * - Circuit breaker is OPEN (fails fast)
 * - All retry attempts exhausted
 * - Geocoding fails for any other reason
 * 
 * Differences from FailedEmailHandler:
 * - Only stores Address ID (not content)
 * - Simpler logic (no content validation)
 * - No "critical" vs "non-critical" distinction
 * 
 * Location: src/main/java/be/steby/CoreProject/bll/domains/address/services/geocoding/fallback/
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FailedGeocodingHandler {

    private final FailedGeocodingRepository failedGeocodingRepository;

    /**
     * Stores a failed geocoding attempt for later retry.
     * 
     * Workflow:
     * 1. Check if address already has a pending retry
     * 2. If yes, skip (avoid duplicates)
     * 3. If no, create new FailedGeocodingEntity with status PENDING
     * 4. Log for monitoring
     * 
     * @param address The address that failed to geocode
     * @param errorMessage Error message from the geocoding failure
     */
    @Transactional
    public void handleFailedGeocoding(Address address, String errorMessage) {
        log.debug("Handling failed geocoding for address ID: {}", address.getId());

        // Check if address already has a pending retry to avoid duplicates
        boolean alreadyPending = failedGeocodingRepository.existsByAddressAndStatus(
                address, 
                FailedGeocodingStatus.PENDING
        );

        if (alreadyPending) {
            log.debug("Address ID {} already has a pending geocoding retry - skipping duplicate", 
                     address.getId());
            return;
        }

        // Create new failed geocoding record
        FailedGeocodingEntity failedGeocoding = FailedGeocodingEntity.builder()
                .address(address)
                .status(FailedGeocodingStatus.PENDING)
                .retryCount(0)
                .errorMessage(errorMessage)
                .build();

        failedGeocodingRepository.save(failedGeocoding);

        log.warn("📍 Failed geocoding stored for retry - Address ID: {}, Error: {}", 
                address.getId(), errorMessage);
    }

    /**
     * Stores a failed geocoding attempt without error message.
     * Convenience method for cases where error details are not available.
     * 
     * @param address The address that failed to geocode
     */
    @Transactional
    public void handleFailedGeocoding(Address address) {
        handleFailedGeocoding(address, "Geocoding failed - no details available");
    }

    /**
     * Gets the count of pending geocoding retries.
     * 
     * Used for monitoring and dashboard metrics.
     * 
     * @return Number of addresses waiting for geocoding retry
     */
    public long getPendingCount() {
        return failedGeocodingRepository.countByStatus(FailedGeocodingStatus.PENDING);
    }

    /**
     * Gets the count of geocoding retries by status.
     * 
     * @param status The status to count
     * @return Count of records with given status
     */
    public long getCountByStatus(FailedGeocodingStatus status) {
        return failedGeocodingRepository.countByStatus(status);
    }
}