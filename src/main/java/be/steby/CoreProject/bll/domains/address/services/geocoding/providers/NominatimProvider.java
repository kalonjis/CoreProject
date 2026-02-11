package be.steby.CoreProject.bll.domains.address.services.geocoding.providers;

import be.steby.CoreProject.bll.domains.address.models.GeocodingResult;
import be.steby.CoreProject.bll.domains.address.services.geocoding.fallback.FailedGeocodingHandler;
import be.steby.CoreProject.dl.entities.Address;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Optional;

/**
 * Nominatim (OpenStreetMap) geocoding provider with Circuit Breaker protection.
 *
 * Features:
 * - Free geocoding service from OpenStreetMap
 * - Rate limiting: 1 request per second (enforced locally)
 * - Circuit Breaker: Fails fast when Nominatim is down
 * - Retry: Conservative retry strategy respecting rate limits
 * - Fallback: Returns empty result when circuit is open
 *
 * Circuit Breaker Configuration:
 * - Failure threshold: 60% (more tolerant than SMTP)
 * - Wait in open state: 120s (Nominatim may have longer outages)
 * - Slow call threshold: 8s
 * - Sliding window: 20 calls
 *
 * API Documentation:
 * https://nominatim.org/release-docs/latest/api/Search/
 *
 * Usage Policy:
 * - Fair use only (1 req/sec max)
 * - Must include User-Agent with contact info
 * - Results licensed under ODbL
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NominatimProvider implements GeocodingProvider {

    private static final String BACKEND_NAME = "nominatimBackend";

    private final NominatimProperties properties;
    private final RestTemplateBuilder restTemplateBuilder;
    private final FailedGeocodingHandler failedGeocodingHandler;

    // Rate limiting: track last API call timestamp
    private long lastCallTimestamp = 0;

    /**
     * Geocodes an address with Circuit Breaker and Retry protection.
     *
     * Execution flow:
     * 1. Circuit Breaker checks if calls are permitted
     * 2. If circuit is OPEN, fallback is called immediately
     * 3. If circuit is CLOSED/HALF_OPEN, attempt geocoding
     * 4. On failure, Retry mechanism may retry (max 1 retry)
     * 5. If all retries fail, fallback is called
     *
     * @param address the address to geocode
     * @return GeocodingResult if successful, empty if circuit open or geocoding failed
     */
    @Override
    @CircuitBreaker(name = BACKEND_NAME, fallbackMethod = "geocodeFallback")
    @Retry(name = BACKEND_NAME)
    public Optional<GeocodingResult> geocode(Address address) {
        log.debug("Geocoding address ID {} with Nominatim (Circuit Breaker protected)", address.getId());

        try {
            // Build query string from address fields
            String query = buildQuery(address);
            if (query.isBlank()) {
                log.warn("Cannot geocode address ID {}: insufficient data", address.getId());
                return Optional.empty();
            }

            // Enforce rate limit before API call (local rate limiting)
            enforceRateLimit();

            // Call Nominatim API
            NominatimResponse response = callNominatimApi(query);

            // Parse and return result
            GeocodingResult result = new GeocodingResult(
                    response.getLatitude(),
                    response.getLongitude(),
                    response.displayName(),
                    getProviderName()
            );

            log.debug("Successfully geocoded address ID {}: [{}, {}]",
                    address.getId(), result.latitude(), result.longitude());

            return Optional.of(result);

        } catch (CallNotPermittedException e) {
            // This should not happen as fallback handles it, but log just in case
            log.warn("Circuit breaker is OPEN for Nominatim - call not permitted for address ID {}",
                    address.getId());
            throw e; // Let fallback handle it

        } catch (Exception e) {
            log.error("Geocoding failed for address ID {}: {} - {}",
                    address.getId(), e.getClass().getSimpleName(), e.getMessage());
            throw e; // Let retry/fallback handle it
        }
    }

    /**
     * Fallback method called when:
     * - Circuit breaker is OPEN (fails fast)
     * - All retry attempts exhausted
     * - Unrecoverable error occurs
     *
     * This method:
     * 1. Stores the failed geocoding attempt for later retry
     * 2. Returns empty result gracefully
     *
     * The address will be retried when:
     * - Circuit breaker transitions to CLOSED
     * - Admin manually triggers retry
     * - Scheduled batch job runs
     *
     * @param address the address that failed to geocode
     * @param throwable the exception that triggered the fallback
     * @return empty Optional (geocoding failed gracefully)
     */
    private Optional<GeocodingResult> geocodeFallback(Address address, Throwable throwable) {
        String errorMessage;

        if (throwable instanceof CallNotPermittedException) {
            errorMessage = "Circuit breaker is OPEN - failing fast";
            log.warn("🔴 Nominatim Circuit Breaker is OPEN - Queueing address ID {} for retry " +
                    "(will retry when circuit closes)", address.getId());
        } else {
            errorMessage = throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
            log.error("⚠️ Geocoding fallback triggered for address ID {} - Error: {} - " +
                    "Queueing for retry", address.getId(), errorMessage);
        }

        // Store failed geocoding attempt for later retry
        failedGeocodingHandler.handleFailedGeocoding(address, errorMessage);

        // Return empty result - address will be retried later
        return Optional.empty();
    }

    @Override
    public String getProviderName() {
        return "Nominatim";
    }

    /**
     * Builds query string from address fields.
     * Format: "street, city, postalCode, country"
     */
    private String buildQuery(Address address) {
        StringBuilder query = new StringBuilder();

        if (address.getStreetNumber() != null && !address.getStreetNumber().isBlank()) {
            query.append(address.getStreetNumber()).append(" ");
        }
        if (address.getStreetName() != null && !address.getStreetName().isBlank()) {
            query.append(address.getStreetName()).append(", ");
        }
        if (address.getCity() != null && !address.getCity().isBlank()) {
            query.append(address.getCity()).append(", ");
        }
        if (address.getPostalCode() != null && !address.getPostalCode().isBlank()) {
            query.append(address.getPostalCode()).append(", ");
        }
        if (address.getCountryCode() != null && !address.getCountryCode().isBlank()) {
            query.append(address.getCountryCode());
        }

        return query.toString().trim();
    }

    /**
     * Enforces rate limit of 1 request per second.
     * Thread-safe implementation using synchronized block.
     *
     * Note: This is local rate limiting only.
     * Nominatim also enforces rate limits server-side.
     */
    private synchronized void enforceRateLimit() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastCallTimestamp;

        if (elapsed < 1000) {
            long sleepTime = 1000 - elapsed;
            log.trace("Rate limiting: sleeping {}ms before Nominatim API call", sleepTime);
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Rate limit sleep interrupted");
            }
        }

        lastCallTimestamp = System.currentTimeMillis();
    }

    /**
     * Calls Nominatim API with configured timeout.
     * Throws exceptions that will be handled by Retry and Circuit Breaker.
     */
    private NominatimResponse callNominatimApi(String query) {
        RestTemplate restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(properties.getTimeout()))
                .setReadTimeout(Duration.ofMillis(properties.getTimeout()))
                .defaultHeader("User-Agent", properties.getUserAgent())
                .build();

        String url = buildApiUrl(query);

        log.trace("Calling Nominatim API: {}", url);

        // Execute API call - exceptions will be caught by Circuit Breaker/Retry
        NominatimResponse[] responses = restTemplate.getForObject(url, NominatimResponse[].class);

        if (responses == null || responses.length == 0) {
            log.warn("Nominatim returned no results for query: {}", query);
            throw new IllegalStateException("No geocoding results found");
        }

        return responses[0]; // Return first (best) result
    }

    /**
     * Builds Nominatim API URL with query parameters.
     */
    private String buildApiUrl(String query) {
        return UriComponentsBuilder.fromHttpUrl(properties.getUrl() + "/search")
                .queryParam("q", query)
                .queryParam("format", "json")
                .queryParam("limit", 1)
                .queryParam("addressdetails", 1)
                .build()
                .toUriString();
    }

    /**
     * Nominatim API response DTO.
     * Only maps fields we need (lat, lon, display_name).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimResponse(
            @JsonProperty("lat") String lat,
            @JsonProperty("lon") String lon,
            @JsonProperty("display_name") String displayName
    ) {
        /**
         * Converts latitude string to Double.
         * @return latitude as Double
         */
        public Double getLatitude() {
            return Double.parseDouble(lat);
        }

        /**
         * Converts longitude string to Double.
         * @return longitude as Double
         */
        public Double getLongitude() {
            return Double.parseDouble(lon);
        }
    }
}