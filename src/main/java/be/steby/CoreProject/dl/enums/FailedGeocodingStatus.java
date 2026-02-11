package be.steby.CoreProject.dl.enums;

/**
 * Status of a failed geocoding attempt.
 * 
 * Lifecycle:
 * 1. PENDING - Initial state when geocoding fails
 * 2. RETRYING - Currently being retried
 * 3. SUCCESS - Retry succeeded, coordinates updated
 * 4. FAILED - All retries exhausted, manual intervention needed
 * 
 * Location: src/main/java/be/steby/CoreProject/dl/enums/
 */
public enum FailedGeocodingStatus {
    
    /**
     * Geocoding failed, waiting for retry.
     * This is the initial state when an address fails to geocode.
     */
    PENDING,
    
    /**
     * Currently being retried by the batch retry service.
     */
    RETRYING,
    
    /**
     * Successfully geocoded on retry.
     * Address now has coordinates.
     */
    SUCCESS,
    
    /**
     * Failed after all retry attempts.
     * Requires manual review or intervention.
     */
    FAILED
}