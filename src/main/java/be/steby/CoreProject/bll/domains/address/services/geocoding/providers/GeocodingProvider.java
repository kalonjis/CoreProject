package be.steby.CoreProject.bll.domains.address.services.geocoding.providers;

import be.steby.CoreProject.bll.domains.address.models.GeocodingResult;
import be.steby.CoreProject.dl.entities.Address;

import java.util.Optional;

/**
 * Interface for geocoding providers.
 * 
 * Implementations should handle:
 * - API calls to external geocoding services
 * - Rate limiting according to provider policies
 * - Response parsing and error handling
 * - Retry strategies if applicable
 * 
 * Supported providers:
 * - Nominatim (OpenStreetMap)
 * - Google Maps API (future)
 * - Other services (extensible)
 */
public interface GeocodingProvider {

    /**
     * Geocodes an address to get its coordinates.
     * 
     * This method should:
     * 1. Build a query from address fields
     * 2. Call the external API respecting rate limits
     * 3. Parse the response to extract latitude/longitude
     * 4. Return coordinates or empty if not found
     * 
     * @param address the address to geocode (must have valid street/city/country data)
     * @return GeocodingResult with coordinates and metadata, or empty if geocoding failed
     */
    Optional<GeocodingResult> geocode(Address address);

    /**
     * Gets the provider name for logging and debugging.
     * 
     * @return the provider identifier (e.g., "Nominatim", "GoogleMaps")
     */
    String getProviderName();


}