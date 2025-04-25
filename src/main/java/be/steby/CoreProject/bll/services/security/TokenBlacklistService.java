package be.steby.CoreProject.bll.services.security;

public interface TokenBlacklistService {

    void blacklistDeviceTokens(Long userId, Long deviceId);

    boolean isDeviceBlacklisted(Long userId, Long deviceId);

    void cleanupExpiredEntries();
}
