package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.bll.services.security.TokenBlacklistService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    // Map thread-safe pour stocker les appareils blacklistés (userId:deviceId -> expiration)
    private final Map<String, Instant> blacklistedDevices = new ConcurrentHashMap<>();

    @Value("${security.jwt.access-token.expiration}")
    private long accessTokenExpiration;

    /**
     * Blacklist tous les tokens d'un appareil spécifique
     */
    @Override
    public void blacklistDeviceTokens(Long userId, Long deviceId){
        String key = userId + ":" + deviceId;
        // Conserver dans la blacklist pour la durée d'un access token + marge de 1 min
        blacklistedDevices.put(key, Instant.now().plusMillis(accessTokenExpiration + 60000));
    }

    /**
     * Vérifie si les tokens d'un appareil sont blacklistés
     */
    @Override
    public boolean isDeviceBlacklisted(Long userId, Long deviceId) {
        String key = userId + ":" + deviceId;
        Instant expiry = blacklistedDevices.get(key);

        if (expiry == null){
            return false;
        }

        // Nettoyage automatique lors de la vérification
        if (Instant.now().isAfter(expiry)){
            blacklistedDevices.remove(key);
            return false;
        }

        return true;
    }



    /**
     * Nettoie périodiquement les entrées expirées (toutes les heures)
     */
    @Override
    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredEntries() {
        Instant now = Instant.now();
        blacklistedDevices.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }

}
