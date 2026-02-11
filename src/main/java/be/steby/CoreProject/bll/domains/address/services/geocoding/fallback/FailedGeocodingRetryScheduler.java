package be.steby.CoreProject.bll.domains.address.services.geocoding.fallback;

import be.steby.CoreProject.bll.domains.address.models.GeocodingResult;
import be.steby.CoreProject.bll.domains.address.services.geocoding.providers.GeocodingProvider;
import be.steby.CoreProject.bll.domains.address.services.geocoding.GeocodingRetryService;
import be.steby.CoreProject.dal.repositories.AddressRepository;
import be.steby.CoreProject.dal.repositories.FailedGeocodingRepository;
import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.FailedGeocodingEntity;
import be.steby.CoreProject.dl.enums.FailedGeocodingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Scheduled job that retries failed geocoding attempts stored in the database.
 *
 * This scheduler complements the circuit breaker recovery mechanism:
 * - Circuit breaker recovery: triggers immediate retry when service recovers
 * - This scheduler: handles cases where geocoding fails without opening circuit
 *
 * IMPORTANT: This scheduler calls geocoding DIRECTLY via the provider,
 * which means failures WILL be recorded by the Circuit Breaker.
 * This is intentional - if Nominatim is consistently failing, the CB should open.
 *
 * Frequency is conservative (every 5 minutes) to respect Nominatim's rate limits.
 *
 * @see GeocodingRetryService
 * @see FailedGeocodingEntity
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FailedGeocodingRetryScheduler {

    private final FailedGeocodingRepository failedGeocodingRepository;
    private final AddressRepository addressRepository;
    private final GeocodingProvider geocodingProvider;

    @Value("${app.geocoding.fallback.database.retry-batch-size:5}")
    private int batchSize;

    @Value("${app.geocoding.fallback.database.max-retries:5}")
    private int maxRetries;

    @Value("${app.geocoding.fallback.database.stuck-threshold-minutes:30}")
    private int stuckThresholdMinutes;

    @Value("${app.geocoding.fallback.database.cleanup-success-after-days:7}")
    private int cleanupSuccessAfterDays;

    /**
     * Main retry job - processes failed geocoding attempts.
     * Runs every 5 minutes by default.
     * 
     * Conservative frequency to respect Nominatim's rate limits (1 req/sec).
     * With batch size of 5 and 1 sec between calls, each run takes ~5 seconds.
     */
    @Scheduled(cron = "${app.geocoding.fallback.database.retry-cron:0 */5 * * * *}")
    @Transactional
    public void retryFailedGeocoding() {
        log.debug("Starting failed geocoding retry job...");

        List<FailedGeocodingEntity> attemptsToRetry = failedGeocodingRepository
                .findByStatusOrderByCreatedAtAsc(FailedGeocodingStatus.PENDING)
                .stream()
                .limit(batchSize)
                .toList();

        if (attemptsToRetry.isEmpty()) {
            log.debug("No failed geocoding ready for retry.");
            return;
        }

        log.info("Found {} geocoding attempt(s) ready for retry.", attemptsToRetry.size());

        int successCount = 0;
        int failedCount = 0;
        int exhaustedCount = 0;

        for (FailedGeocodingEntity attempt : attemptsToRetry) {
            try {
                RetryResult result = processGeocoding(attempt);
                switch (result) {
                    case SUCCESS -> successCount++;
                    case FAILED -> failedCount++;
                    case EXHAUSTED -> exhaustedCount++;
                }
                
                // Respect Nominatim rate limit: 1 request per second
                Thread.sleep(1100);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Geocoding retry job interrupted");
                break;
            }
        }

        log.info("Geocoding retry job completed - Success: {}, Failed: {}, Exhausted: {}", 
                successCount, failedCount, exhaustedCount);
    }

    /**
     * Processes a single failed geocoding attempt.
     *
     * @param attempt the geocoding attempt to retry
     * @return result of the retry
     */
    private RetryResult processGeocoding(FailedGeocodingEntity attempt) {
        Long addressId = attempt.getAddress().getId();

        // Check if max retries reached
        if (!attempt.canRetry(maxRetries)) {
            log.warn("Max retry attempts reached for address ID: {} - Marking as FAILED", addressId);
            attempt.markAsFailed("Max retry attempts (" + maxRetries + ") reached");
            failedGeocodingRepository.save(attempt);
            return RetryResult.EXHAUSTED;
        }

        // Mark as retrying (optimistic lock)
        attempt.markAsRetrying();
        failedGeocodingRepository.save(attempt);

        try {
            // Reload address to avoid LazyInitializationException
            Address address = addressRepository.findById(addressId)
                    .orElseThrow(() -> new IllegalStateException("Address not found: " + addressId));

            log.info("Retrying geocoding for address ID: {}, Attempt: {}/{}", 
                    addressId, attempt.getRetryCount(), maxRetries);

            // Attempt geocoding (goes through Circuit Breaker)
            Optional<GeocodingResult> result = geocodingProvider.geocode(address);

            if (result.isPresent()) {
                // Success! Update address
                GeocodingResult geocodingResult = result.get();
                address.setLatitude(geocodingResult.latitude());
                address.setLongitude(geocodingResult.longitude());
                address.setValidated(true);
                
                if (geocodingResult.formattedAddress() != null) {
                    address.setFormattedAddress(geocodingResult.formattedAddress());
                }
                
                addressRepository.save(address);

                // Mark as successful
                attempt.markAsSuccess();
                failedGeocodingRepository.save(attempt);

                log.info("✅ Geocoding retry SUCCESS for address ID: {} - Coordinates: [{}, {}]",
                        addressId, geocodingResult.latitude(), geocodingResult.longitude());

                return RetryResult.SUCCESS;

            } else {
                // Geocoding returned empty (might be circuit breaker open)
                String errorMsg = "Geocoding returned no results";
                attempt.resetToPending(errorMsg);
                failedGeocodingRepository.save(attempt);

                log.warn("⚠️ Geocoding retry returned no results for address ID: {} (attempt {}/{})",
                        addressId, attempt.getRetryCount(), maxRetries);

                return RetryResult.FAILED;
            }

        } catch (Exception e) {
            // Exception during geocoding
            String errorMsg = e.getClass().getSimpleName() + ": " + e.getMessage();
            attempt.resetToPending(errorMsg);
            failedGeocodingRepository.save(attempt);

            log.error("❌ Geocoding retry FAILED for address ID: {} - Error: {} (attempt {}/{})",
                    addressId, errorMsg, attempt.getRetryCount(), maxRetries);

            return RetryResult.FAILED;
        }
    }

    /**
     * Cleanup job - removes old successfully geocoded records.
     * Runs daily at 4 AM (after other cleanup jobs at 3 AM).
     */
    @Scheduled(cron = "${app.geocoding.fallback.database.cleanup-cron:0 0 4 * * *}")
    @Transactional
    public void cleanupOldSuccessRecords() {
        log.debug("Starting geocoding cleanup job...");

        LocalDateTime cutoff = LocalDateTime.now().minusDays(cleanupSuccessAfterDays);
        int deleted = failedGeocodingRepository.deleteOldSuccessRecords(
                FailedGeocodingStatus.SUCCESS, 
                cutoff
        );

        if (deleted > 0) {
            log.info("Cleaned up {} old successful geocoding records.", deleted);
        } else {
            log.debug("No old geocoding records to clean up.");
        }
    }

    /**
     * Recovery job - resets stuck RETRYING records back to PENDING.
     * Handles recovery after application crash.
     * Runs every 30 minutes.
     */
    @Scheduled(cron = "${app.geocoding.fallback.database.stuck-check-cron:0 */30 * * * *}")
    @Transactional
    public void resetStuckGeocoding() {
        log.debug("Checking for stuck geocoding attempts...");

        LocalDateTime stuckSince = LocalDateTime.now().minusMinutes(stuckThresholdMinutes);
        int reset = failedGeocodingRepository.resetStuckRecords(stuckSince);

        if (reset > 0) {
            log.warn("Reset {} stuck geocoding attempt(s) back to PENDING status.", reset);
        } else {
            log.debug("No stuck geocoding attempts found.");
        }
    }

    /**
     * Result of a geocoding retry attempt.
     */
    private enum RetryResult {
        SUCCESS,
        FAILED,
        EXHAUSTED
    }
}