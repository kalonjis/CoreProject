package be.steby.CoreProject.bll.domains.address.services.geocoding;

/**
 * Service for address geocoding operations.
 * 
 * Responsibilities:
 * - Orchestrate geocoding workflow
 * - Delegate external API calls to GeocodingProvider
 * - Update Address entity with coordinates
 * - Handle business logic and validation
 * 
 * This service is typically called asynchronously by AddressGeocodingListener
 * to avoid blocking address creation operations.
 */
public interface GeocodingService {

    /**
     * Geocodes an address and updates it with coordinates.
     * 
     * Workflow:
     * 1. Fetch address from database
     * 2. Call geocoding provider (Nominatim)
     * 3. Update address with latitude/longitude
     * 4. Mark address as validated
     * 5. Save formatted address from provider
     * 
     * Error handling:
     * - If address not found: throws exception
     * - If geocoding fails: logs warning, does not throw
     * - If update fails: throws exception
     * 
     * @param addressId the ID of the address to geocode
     * @throws IllegalArgumentException if address ID is null or not found
     */
    void geocodeAddress(Long addressId);
}