package be.steby.CoreProject.bll.domains.address.events;

/**
 * Event published when an address is created and requires geocoding.
 * 
 * This event triggers asynchronous geocoding processing to fetch
 * latitude/longitude coordinates from an external provider (Nominatim).
 * 
 * Processing:
 * - Listened by AddressGeocodingListener
 * - Executed on dedicated geocodingExecutor thread pool
 * - Non-blocking: does not impact address creation response time
 * 
 * @param addressId the ID of the address to geocode
 */
public record AddressGeocodingRequestedEvent(Long addressId) {
}