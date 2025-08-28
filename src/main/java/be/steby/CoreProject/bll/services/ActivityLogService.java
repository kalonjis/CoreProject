package be.steby.CoreProject.bll.services;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Interface pour les méthodes transversales de log (optionnel)
 * Peut servir pour des requêtes cross-domain UNIQUEMENT
 */
public interface ActivityLogService {

    // ================== QUERY METHODS TRANSVERSALES SEULEMENT ==================

    /**
     * Historique complet utilisateur (tous domaines)
     */
    Page<ActivityLog> getUserConnectionHistory(User user, Pageable pageable);

    /**
     * Recherche cross-domain
     */
    Page<ActivityLog> searchLogs(Long userId, String ipAddress,
                                 Instant startDate, Instant endDate, Pageable pageable);

    /**
     * Stats système (tous domaines)
     */
    Map<String, Object> getSystemStats(Instant startDate, Instant endDate);

    /**
     * Nettoyage automatique (tous domaines)
     */
    void cleanupOldLogs();
}
