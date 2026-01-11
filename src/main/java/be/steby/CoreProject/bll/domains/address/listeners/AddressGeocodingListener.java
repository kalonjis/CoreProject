package be.steby.CoreProject.bll.domains.address.listeners;

import be.steby.CoreProject.bll.domains.address.events.AddressGeocodingRequestedEvent;
import be.steby.CoreProject.bll.domains.address.services.geocoding.GeocodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Asynchronous listener for address geocoding events.
 *
 * Workflow:
 * 1. Listens to AddressGeocodingRequestedEvent AFTER transaction commit
 * 2. Delegates geocoding to GeocodingService
 * 3. Handles errors gracefully (no exception propagation)
 *
 * Configuration:
 * - Executes on dedicated "geocodingExecutor" thread pool
 * - Uses @TransactionalEventListener with AFTER_COMMIT phase
 * - Does not block the main thread or API response
 * - Fault-tolerant: geocoding failures don't crash the system
 *
 * Transaction handling:
 * The AFTER_COMMIT phase ensures the address is committed to the database
 * before the async listener attempts to fetch and geocode it. This prevents
 * "entity not found" errors in concurrent scenarios.
 *
 * Error handling:
 * - Logs errors at WARN level
 * - Does not retry (handled by GeocodingService if needed)
 * - Address remains valid even if geocoding fails
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AddressGeocodingListener {

    private final GeocodingService geocodingService;

    /**
     * Handles address geocoding request events asynchronously.
     *
     * This method is executed on a separate thread pool (geocodingExecutor)
     * to avoid blocking the main application flow.
     *
     * The listener uses AFTER_COMMIT phase to ensure the address is committed
     * to the database before attempting to geocode it. This prevents
     * "entity not found" errors when the async thread tries to fetch the address.
     *
     * @param event the geocoding request event containing the address ID
     */
    @Async("geocodingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGeocodingRequest(AddressGeocodingRequestedEvent event) {
        log.info("Geocoding requested for address ID: {}", event.addressId());

        try {
            geocodingService.geocodeAddress(event.addressId());
            log.info("Geocoding completed successfully for address ID: {}", event.addressId());

        } catch (Exception e) {
            // Fault-tolerant: log error but don't propagate exception
            // Address remains valid even without coordinates
            log.warn("Geocoding failed for address ID: {} - Error: {}",
                    event.addressId(), e.getMessage());
        }
    }
}