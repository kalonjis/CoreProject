package be.steby.CoreProject.bll.domains.device.utils;

import be.steby.CoreProject.dl.entities.Device;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Cached device information with expiration handling
 */
@Data
@RequiredArgsConstructor
public class CachedDevice {

    private final Device device;
    private final long cacheTime;

    public CachedDevice(Device device) {
        this.device = device;
        this.cacheTime = System.currentTimeMillis();
    }

    /**
     * Check if this cached device has expired
     * @param expirationTimeMs Cache expiration time in milliseconds
     * @return true if expired, false otherwise
     */
    public boolean isExpired(long expirationTimeMs) {
        return (System.currentTimeMillis() - cacheTime) > expirationTimeMs;
    }
}