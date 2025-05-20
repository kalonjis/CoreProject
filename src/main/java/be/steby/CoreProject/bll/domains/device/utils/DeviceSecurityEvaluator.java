package be.steby.CoreProject.bll.domains.device.utils;

import be.steby.CoreProject.bll.events.device.DeviceTrustLevelChangedEvent;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class DeviceSecurityEvaluator {

    private final DeviceService deviceService;
    private final Map<Long, CachedDeviceInfo> deviceCache = new ConcurrentHashMap<>();

    @Value("${device.cache.expiration:60000}") // 1 minute par défaut
    private long cacheExpirationMs;

    /**
     * Vérifie si l'appareil a le niveau de confiance requis.
     * @param authentication L'authentification en cours
     * @param request La requête HTTP
     * @param requiredLevel Le niveau de confiance requis
     * @return true si l'appareil a le niveau de confiance requis, false sinon
     */
    public boolean hasRequiredTrustLevel(Authentication authentication, HttpServletRequest request, DeviceTrustLevel requiredLevel) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Claims claims = (Claims) request.getAttribute("jwt_claims");
        if (claims == null) {
            return false;
        }

        Long deviceId = claims.get("deviceId", Long.class);
        if (deviceId == null) {
            return false;
        }

        DeviceTrustLevel currentLevel = getDeviceTrustLevel(deviceId);
        if (currentLevel == null) {
            return false;
        }

        return currentLevel.ordinal() >= requiredLevel.ordinal();
    }

    /**
     * Récupère le niveau de confiance d'un appareil, en vérifiant d'abord le cache
     * puis en interrogeant la base de données si nécessaire.
     */
    private DeviceTrustLevel getDeviceTrustLevel(Long deviceId) {
        CachedDeviceInfo cachedInfo = deviceCache.get(deviceId);
        if (cachedInfo != null && !cachedInfo.isExpired()) {
            return cachedInfo.getTrustLevel();
        }

        // Si non trouvé dans le cache ou expiré, récupérer depuis la base de données
        Device device = deviceService.getDeviceById(deviceId);
        if (device == null) {
            return null;
        }

        // Mettre à jour le cache
        deviceCache.put(deviceId, new CachedDeviceInfo(device.getDeviceTrustLevel()));

        return device.getDeviceTrustLevel();
    }

    /**
     * Écouteur d'événements pour mettre à jour le cache lors d'un changement de niveau de confiance.
     */
    @EventListener
    public void handleDeviceTrustLevelChangedEvent(DeviceTrustLevelChangedEvent event) {
        deviceCache.put(event.deviceId(), new CachedDeviceInfo(event.newTrustLevel()));
    }

    @Data
    private class CachedDeviceInfo {
        private final DeviceTrustLevel trustLevel;
        private final long timestamp;

        public CachedDeviceInfo(DeviceTrustLevel trustLevel) {
            this.trustLevel = trustLevel;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > cacheExpirationMs;
        }
    }
}
