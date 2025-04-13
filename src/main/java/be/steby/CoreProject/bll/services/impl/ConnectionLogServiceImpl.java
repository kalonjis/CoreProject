package be.steby.CoreProject.bll.services.impl;

import be.steby.CoreProject.bll.services.ConnectionLogService;
import be.steby.CoreProject.dal.repositories.ConnectionLogRepository;
import be.steby.CoreProject.dl.entities.ConnectionLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectionLogServiceImpl implements ConnectionLogService {
    private final ConnectionLogRepository connectionLogRepository;

    /**
     * Enregistre une tentative de connexion (réussie ou échouée)
     *
     * @param user         L'utilisateur concerné
     * @param device       L'appareil utilisé (peut être null)
     * @param successful   Si la connexion a réussi
     * @param failureReason Raison de l'échec (si applicable)
     * @param request      La requête HTTP
     * @return L'entité ConnectionLog enregistrée
     */
    @Transactional
    public ConnectionLog logLoginAttempt(User user, Device device, boolean successful,
                                         String failureReason, HttpServletRequest request) {

        ConnectionLog connectionLog = ConnectionLog.builder()
                .user(user)
                .device(device)
                .timestamp(Instant.now())
                .ipAddress(getClientIpAddress(request))
                .successful(successful)
                .failureReason(failureReason)
                .actionType("LOGIN")
                .build();

        return connectionLogRepository.save(connectionLog);
    }

    /**
     * Enregistre une déconnexion
     */
    @Transactional
    public ConnectionLog logLogout(User user, Device device, HttpServletRequest request) {
        ConnectionLog connectionLog = ConnectionLog.builder()
                .user(user)
                .device(device)
                .timestamp(Instant.now())
                .ipAddress(getClientIpAddress(request))
                .successful(true)
                .actionType("LOGOUT")
                .build();

        return connectionLogRepository.save(connectionLog);
    }

    /**
     * Enregistre une action de sécurité (changement de mot de passe, reset, etc.)
     */
    @Transactional
    public ConnectionLog logSecurityAction(User user, Device device, String actionType,
                                           boolean successful, String details, HttpServletRequest request) {

        ConnectionLog connectionLog = ConnectionLog.builder()
                .user(user)
                .device(device)
                .timestamp(Instant.now())
                .ipAddress(getClientIpAddress(request))
                .successful(successful)
                .failureReason(successful ? null : details)
                .actionType(actionType)
                .build();

        return connectionLogRepository.save(connectionLog);
    }

    /**
     * Obtient l'historique de connexion d'un utilisateur
     */
    @Transactional(readOnly = true)
    public Page<ConnectionLog> getUserConnectionHistory(User user, Pageable pageable) {
        return connectionLogRepository.findByUserOrderByTimestampDesc(user, pageable);
    }

    /**
     * Obtient les dernières tentatives de connexion d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<ConnectionLog> getRecentLoginAttempts(User user) {
        return connectionLogRepository.findTop10ByUserAndSuccessfulTrueOrderByTimestampDesc(user);
    }

    /**
     * Tâche planifiée pour supprimer les anciens logs (après 180 jours par défaut)
     */
    @Transactional
    @Scheduled(cron = "0 0 0 * * ?") // Tous les jours à minuit
    public void cleanupOldLogs() {
        // Par défaut, conserve 6 mois de logs
        Instant cutoffDate = Instant.now().minus(180, ChronoUnit.DAYS);
        connectionLogRepository.deleteByTimestampBefore(cutoffDate);
        log.info("Nettoyage des logs de connexion antérieurs à {}", cutoffDate);
    }

    /**
     * Détermine l'adresse IP du client à partir de la requête HTTP
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // En cas de plusieurs proxies, la première IP est celle du client
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

