package be.steby.CoreProject.bll.domains.address.services.geocoding.providers;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Nominatim geocoding provider.
 * 
 * Maps values from application.yml under the 'geocoding.nominatim' prefix.
 * 
 * Example configuration:
 * <pre>
 * geocoding:
 *   nominatim:
 *     url: https://nominatim.openstreetmap.org
 *     user-agent: YourApp/1.0 (contact@example.com)
 *     timeout: 5000
 *     retry-enabled: true
 * </pre>
 * 
 * Nominatim usage policy:
 * - Rate limit: 1 request per second (enforced by provider)
 * - User-Agent header is mandatory
 * - Free tier for fair use
 * 
 * @see <a href="https://operations.osmfoundation.org/policies/nominatim/">Nominatim Usage Policy</a>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "geocoding.nominatim")
public class NominatimProperties {

    /**
     * Base URL of the Nominatim API.
     * Default: https://nominatim.openstreetmap.org
     */
    private String url = "https://nominatim.openstreetmap.org";

    /**
     * User-Agent header value (mandatory for Nominatim).
     * Should include app name and contact email.
     */
    private String userAgent = "SpringBootApp/1.0";

    /**
     * HTTP request timeout in milliseconds.
     * Default: 5000ms (5 seconds)
     */
    private int timeout = 5000;

    /**
     * Enable retry on transient failures (timeout, 503).
     * Default: true
     */
    private boolean retryEnabled = true;
}