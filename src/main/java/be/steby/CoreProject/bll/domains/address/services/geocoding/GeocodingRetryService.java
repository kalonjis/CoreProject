package be.steby.CoreProject.bll.domains.address.services.geocoding;

import be.steby.CoreProject.bll.domains.address.models.GeocodingResult;
import be.steby.CoreProject.bll.domains.address.services.geocoding.providers.GeocodingProvider;
import be.steby.CoreProject.dal.repositories.AddressRepository;
import be.steby.CoreProject.dal.repositories.FailedGeocodingRepository;
import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.FailedGeocodingEntity;
import be.steby.CoreProject.dl.enums.FailedGeocodingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for retrying failed geocoding attempts.
 *
 * Responsibilities:
 * - Process batch of pending failed geocoding attempts
 * - Retry geocoding using the GeocodingProvider
 * - Update address with coordinates on success
 * - Update failed geocoding status (SUCCESS/FAILED/PENDING)
 * - Respect max retry attempts (default: 5)
 *
 * Triggered by:
 * - Circuit breaker state transition (OPEN → HALF_OPEN → CLOSED)
 * - Manual admin API call
 * - Scheduled batch job (optional)
 *
 * Location: src/main/java/be/steby/CoreProject/bll/domains/address/services/geocoding/
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodingRetryService {

    private static final int MAX_RETRY_ATTEMPTS = 5;

    private final FailedGeocodingRepository failedGeocodingRepository;
    private final AddressRepository addressRepository;
    private final GeocodingProvider geocodingProvider;

    /**
     * Processes all pending failed geocoding attempts asynchronously.
     *
     * Workflow:
     * 1. Fetch all PENDING failed geocoding records
     * 2. For each record:
     *    a. Check if retry limit reached
     *    b. Attempt geocoding via provider
     *    c. Update address with coordinates on success
     *    d. Update failed geocoding status
     * 3. Log summary statistics
     *
     * This method is async to avoid blocking the circuit breaker event listener.
     */
    @Async("geocodingExecutor")
    public void retryAllPendingGeocoding() {
        log.info("🔄 Starting batch retry for failed geocoding attempts");

        List<FailedGeocodingEntity> pendingAttempts = failedGeocodingRepository
                .findByStatus(FailedGeocodingStatus.PENDING);

        if (pendingAttempts.isEmpty()) {
            log.info("No pending geocoding attempts to retry");
            return;
        }

        log.info("Found {} pending geocoding attempts to retry", pendingAttempts.size());

        int successCount = 0;
        int failedCount = 0;
        int exhaustedCount = 0;

        for (FailedGeocodingEntity attempt : pendingAttempts) {
            try {
                // Extract address ID before entering transaction to avoid lazy loading issues
                Long addressId = attempt.getAddress().getId();
                Long attemptId = attempt.getId();

                RetryResult result = retrySingleGeocoding(attemptId, addressId);

                switch (result) {
                    case SUCCESS -> successCount++;
                    case FAILED -> failedCount++;
                    case EXHAUSTED -> exhaustedCount++;
                }

                // Respect Nominatim rate limit: 1 request per second
                // (NominatimProvider also enforces this, but belt-and-suspenders approach)
                Thread.sleep(1100);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Geocoding retry batch interrupted");
                break;
            } catch (Exception e) {
                log.error("Unexpected error retrying geocoding for attempt ID {}: {}",
                        attempt.getId(), e.getMessage(), e);
                failedCount++;
            }
        }

        log.info("✅ Batch geocoding retry completed - Success: {}, Failed: {}, Exhausted: {}",
                successCount, failedCount, exhaustedCount);
    }

    /**
     * Retries geocoding for a single failed attempt.
     *
     * This method reloads both the FailedGeocodingEntity and Address from the database
     * to ensure they are fully initialized within the current transaction.
     *
     * @param attemptId The ID of the failed geocoding attempt
     * @param addressId The ID of the address to geocode
     * @return Result of the retry attempt
     */
    @Transactional
    protected RetryResult retrySingleGeocoding(Long attemptId, Long addressId) {
        // Reload entities within this transaction to avoid LazyInitializationException
        FailedGeocodingEntity attempt = failedGeocodingRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalStateException("Failed geocoding attempt not found: " + attemptId));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalStateException("Address not found: " + addressId));

        log.debug("Retrying geocoding for address ID: {} (attempt {}/{})",
                addressId, attempt.getRetryCount() + 1, MAX_RETRY_ATTEMPTS);

        // Check if max retries reached
        if (!attempt.canRetry(MAX_RETRY_ATTEMPTS)) {
            log.warn("Max retry attempts reached for address ID: {} - Marking as FAILED",
                    addressId);
            attempt.markAsFailed("Max retry attempts (" + MAX_RETRY_ATTEMPTS + ") reached");
            failedGeocodingRepository.save(attempt);
            return RetryResult.EXHAUSTED;
        }

        // Mark as retrying
        attempt.markAsRetrying();
        failedGeocodingRepository.save(attempt);

        try {
            // Attempt geocoding
            Optional<GeocodingResult> result = geocodingProvider.geocode(address);

            if (result.isPresent()) {
                // Success! Update address with coordinates
                GeocodingResult geocodingResult = result.get();
                address.setLatitude(geocodingResult.latitude());
                address.setLongitude(geocodingResult.longitude());
                address.setValidated(true);

                if (geocodingResult.formattedAddress() != null) {
                    address.setFormattedAddress(geocodingResult.formattedAddress());
                }

                addressRepository.save(address);

                // Mark geocoding as successful
                attempt.markAsSuccess();
                failedGeocodingRepository.save(attempt);

                log.info("✅ Geocoding retry SUCCESS for address ID: {} - Coordinates: [{}, {}]",
                        addressId, geocodingResult.latitude(), geocodingResult.longitude());

                return RetryResult.SUCCESS;

            } else {
                // Geocoding returned empty - reset to pending for next batch
                String errorMsg = "Geocoding returned no results";
                attempt.resetToPending(errorMsg);
                failedGeocodingRepository.save(attempt);

                log.warn("⚠️ Geocoding retry returned no results for address ID: {} " +
                        "(attempt {}/{})", addressId, attempt.getRetryCount(), MAX_RETRY_ATTEMPTS);

                return RetryResult.FAILED;
            }

        } catch (Exception e) {
            // Exception during geocoding - reset to pending for next batch
            String errorMsg = e.getClass().getSimpleName() + ": " + e.getMessage();
            attempt.resetToPending(errorMsg);
            failedGeocodingRepository.save(attempt);

            log.error("❌ Geocoding retry FAILED for address ID: {} - Error: {} " +
                            "(attempt {}/{})", addressId, errorMsg,
                    attempt.getRetryCount(), MAX_RETRY_ATTEMPTS);

            return RetryResult.FAILED;
        }
    }

    /**
     * Manually retries geocoding for a specific address.
     *
     * Used by admin API to force retry a single address.
     *
     * @param addressId The ID of the address to retry
     * @return true if retry was successful
     */
    @Transactional
    public boolean retryGeocodingForAddress(Long addressId) {
        log.info("Manual geocoding retry requested for address ID: {}", addressId);

        // Find pending failed geocoding record
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));

        Optional<FailedGeocodingEntity> attemptOpt = failedGeocodingRepository
                .findByAddressAndStatus(address, FailedGeocodingStatus.PENDING);

        if (attemptOpt.isEmpty()) {
            log.warn("No pending geocoding retry found for address ID: {}", addressId);
            return false;
        }

        FailedGeocodingEntity attempt = attemptOpt.get();
        RetryResult result = retrySingleGeocoding(attempt.getId(), addressId);
        return result == RetryResult.SUCCESS;
    }

    /**
     * Result of a geocoding retry attempt.
     */
    protected enum RetryResult {
        /** Geocoding succeeded, address updated with coordinates */
        SUCCESS,

        /** Geocoding failed, will retry later */
        FAILED,

        /** Max retries exhausted, marked as permanently failed */
        EXHAUSTED
    }
}