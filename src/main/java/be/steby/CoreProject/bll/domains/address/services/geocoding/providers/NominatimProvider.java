package be.steby.CoreProject.bll.domains.address.services.geocoding.providers;

import be.steby.CoreProject.bll.domains.address.models.GeocodingResult;
import be.steby.CoreProject.dl.entities.Address;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
 * Nominatim (OpenStreetMap) geocoding provider implementation.
 *
 * Features:
 * - Free geocoding service from OpenStreetMap
 * - Rate limiting: 1 request per second (enforced)
 * - Retry on transient failures (timeout, 503)
 * - User-Agent header mandatory
 *
 * API Documentation:
 * https://nominatim.org/release-docs/latest/api/Search/
 *
 * Usage policy:
 * - Fair use only (1 req/sec)
 * - Must include User-Agent with contact info
 * - Results licensed under ODbL
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NominatimProvider implements GeocodingProvider {

    private final NominatimProperties properties;
    private final RestTemplateBuilder restTemplateBuilder;

    // Rate limiting: track last API call timestamp
    private long lastCallTimestamp = 0;

    @Override
    public Optional<GeocodingResult> geocode(Address address) {
        log.debug("Geocoding address ID {} with Nominatim", address.getId());

        try {
            // Build query string from address fields
            String query = buildQuery(address);
            if (query.isBlank()) {
                log.warn("Cannot geocode address ID {}: insufficient data", address.getId());
                return Optional.empty();
            }

            // Enforce rate limit before API call
            enforceRateLimit();

            // Call Nominatim API with retry
            NominatimResponse response = callNominatimApi(query);

            // Parse and return result
            return Optional.of(new GeocodingResult(
                    response.getLatitude(),
                    response.getLongitude(),
                    response.displayName(),
                    getProviderName()
            ));

        } catch (Exception e) {
            log.error("Geocoding failed for address ID {}: {}", address.getId(), e.getMessage());
            return Optional.empty();
        }
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
     */
    private synchronized void enforceRateLimit() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastCallTimestamp;

        if (elapsed < 1000) {
            long sleepTime = 1000 - elapsed;
            log.debug("Rate limiting: sleeping {}ms", sleepTime);
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
     * Calls Nominatim API with configured timeout and retry.
     */
    private NominatimResponse callNominatimApi(String query) {
        RestTemplate restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(properties.getTimeout()))
                .setReadTimeout(Duration.ofMillis(properties.getTimeout()))
                .build();

        String url = buildApiUrl(query);

        try {
            return executeApiCall(restTemplate, url);

        } catch (ResourceAccessException | HttpServerErrorException e) {
            // Retry once on transient errors if enabled
            if (properties.isRetryEnabled() && isRetryableError(e)) {
                log.debug("Retrying Nominatim API call after transient error");
                try {
                    Thread.sleep(2000); // Wait 2 seconds before retry
                    return executeApiCall(restTemplate, url);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
            throw e;
        }
    }

    /**
     * Executes the actual HTTP call to Nominatim API.
     */
    private NominatimResponse executeApiCall(RestTemplate restTemplate, String url) {
        log.debug("Calling Nominatim API: {}", url);

        NominatimResponse[] responses = restTemplate.getForObject(url, NominatimResponse[].class);

        if (responses == null || responses.length == 0) {
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
     * Checks if error is retryable (timeout or server error).
     */
    private boolean isRetryableError(Exception e) {
        if (e instanceof ResourceAccessException) {
            return true; // Timeout
        }
        if (e instanceof HttpServerErrorException serverError) {
            return serverError.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE; // 503
        }
        return false;
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