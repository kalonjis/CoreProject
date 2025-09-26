package be.steby.CoreProject.bll.domains.device.cache.models;

import be.steby.CoreProject.dl.entities.Device;
import lombok.Data;

/**
 * Value Object representing a cached device entry with security validation.
 *
 * This model encapsulates:
 * - Device entity reference
 * - Security fingerprint validation
 * - Expiration and LRU tracking
 * - Smart update capabilities
 *
 * Used exclusively by the device cache subsystem.
 */
@Data
public class CachedDeviceInfo {
    private Device device;                    // Not final - allows updates
    private final String fingerprint;
    private long timestamp;                   // Not final - allows refresh
    private final long expirationMs;
    private long lastAccessTime;              // For LRU tracking

    public CachedDeviceInfo(Device device, String fingerprint, long expirationMs) {
        this.device = device;
        this.fingerprint = fingerprint;
        this.timestamp = System.currentTimeMillis();
        this.expirationMs = expirationMs;
        this.lastAccessTime = this.timestamp;
    }

    /**
     * Checks if this cache entry has expired.
     * Uses configured expiration time, not hardcoded value.
     */
    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > expirationMs;
    }

    /**
     * Validates if the provided fingerprint matches the cached one.
     * Critical for security - prevents cache poisoning attacks.
     */
    public boolean matchesFingerprint(String fingerprint) {
        return this.fingerprint != null && this.fingerprint.equals(fingerprint);
    }

    /**
     * Updates the device and refreshes timestamp without recreating the object.
     * Optimizes memory usage by avoiding unnecessary allocations.
     */
    public void updateDevice(Device newDevice) {
        this.device = newDevice;
        this.timestamp = System.currentTimeMillis(); // Refresh expiration
        this.lastAccessTime = this.timestamp;        // Mark as recently used
    }

    /**
     * Marks this entry as recently accessed for LRU tracking.
     * Called when cache hit occurs.
     */
    public void markAccessed() {
        this.lastAccessTime = System.currentTimeMillis();
    }

    /**
     * Refreshes just the timestamp without changing the device.
     * Useful when extending cache lifetime without data changes.
     */
    public void refreshTimestamp() {
        this.timestamp = System.currentTimeMillis();
        this.lastAccessTime = this.timestamp;
    }
}