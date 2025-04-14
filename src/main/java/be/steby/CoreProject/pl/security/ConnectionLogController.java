package be.steby.CoreProject.pl.security;

import be.steby.CoreProject.bll.services.ConnectionLogService;
import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.bll.services.security.SecurityService;
import be.steby.CoreProject.dl.entities.ConnectionLog;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.ActionLogType;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import be.steby.CoreProject.il.device.RequiresDeviceTrustLevel;
import be.steby.CoreProject.pl.security.models.ConnectionLogDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur pour gérer les journaux de connexion et d'activité des utilisateurs
 */
@RestController
@RequestMapping("/api/security/logs")
@RequiredArgsConstructor
public class ConnectionLogController {

    private final ConnectionLogService connectionLogService;
    private final SecurityService securityService;
    private final UserService userService;

    /**
     * Obtient l'historique de connexion de l'utilisateur courant
     */
    @GetMapping("/my-history")
    public ResponseEntity<Page<ConnectionLogDTO>> getMyConnectionHistory(
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        User currentUser = securityService.getAuthenticatedUser();
        Page<ConnectionLog> logs = connectionLogService.getUserConnectionHistory(currentUser, pageable);
        Page<ConnectionLogDTO> dtoPage = logs.map(ConnectionLogDTO::fromEntity);

        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Obtient les dernières connexions réussies de l'utilisateur courant
     */
    @GetMapping("/my-recent-logins")
    public ResponseEntity<List<ConnectionLogDTO>> getMyRecentLogins() {
        User currentUser = securityService.getAuthenticatedUser();
        List<ConnectionLog> recentLogs = connectionLogService.getRecentLoginAttempts(currentUser);

        List<ConnectionLogDTO> dtoList = recentLogs.stream()
                .map(ConnectionLogDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    /**
     * Recherche les actions de l'utilisateur courant par type
     */
    @GetMapping("/my-actions")
    public ResponseEntity<Page<ConnectionLogDTO>> getMyActions(
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        User currentUser = securityService.getAuthenticatedUser();
        List<ActionLogType> actionTypes = parseActionTypes(types);

        Instant startDate = from != null
                ? from.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);

        Instant endDate = to != null
                ? to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now();

        Page<ConnectionLog> logs = connectionLogService.getUserActionHistory(
                currentUser, actionTypes, startDate, endDate, pageable);

        Page<ConnectionLogDTO> dtoPage = logs.map(ConnectionLogDTO::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Obtient des statistiques sur l'activité de l'utilisateur courant
     */
    @GetMapping("/my-stats")
    public ResponseEntity<Map<String, Object>> getMyActivityStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        User currentUser = securityService.getAuthenticatedUser();

        Instant startDate = from != null
                ? from.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);

        Instant endDate = to != null
                ? to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now();

        Map<String, Object> stats = connectionLogService.getUserActivityStats(
                currentUser, startDate, endDate);

        return ResponseEntity.ok(stats);
    }

    /**
     * Vérifie s'il y a des activités suspectes pour l'utilisateur courant
     */
    @GetMapping("/my-security-alerts")
    public ResponseEntity<List<ConnectionLogDTO>> getMySecurityAlerts() {
        User currentUser = securityService.getAuthenticatedUser();
        List<ConnectionLog> suspiciousLogs = connectionLogService.detectSuspiciousActivity(currentUser);

        List<ConnectionLogDTO> dtoList = suspiciousLogs.stream()
                .map(ConnectionLogDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    /**
     * Endpoint pour les administrateurs - obtient l'historique de connexion d'un utilisateur spécifique
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequiresDeviceTrustLevel(DeviceTrustLevel.TRUSTED)
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ConnectionLogDTO>> getUserConnectionHistory(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        User user = userService.getUserById(userId);
        Page<ConnectionLog> logs = connectionLogService.getUserConnectionHistory(user, pageable);

        Page<ConnectionLogDTO> dtoPage = logs.map(ConnectionLogDTO::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Endpoint pour les administrateurs - obtient les actions d'un utilisateur spécifique par type
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequiresDeviceTrustLevel(DeviceTrustLevel.TRUSTED)
    @GetMapping("/user/{userId}/actions")
    public ResponseEntity<Page<ConnectionLogDTO>> getUserActions(
            @PathVariable Long userId,
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        User user = userService.getUserById(userId);
        List<ActionLogType> actionTypes = parseActionTypes(types);

        Instant startDate = from != null
                ? from.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);

        Instant endDate = to != null
                ? to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now();

        Page<ConnectionLog> logs = connectionLogService.getUserActionHistory(
                user, actionTypes, startDate, endDate, pageable);

        Page<ConnectionLogDTO> dtoPage = logs.map(ConnectionLogDTO::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Endpoint pour les administrateurs - recherche avancée des logs
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequiresDeviceTrustLevel(DeviceTrustLevel.HIGHLY_TRUSTED)
    @GetMapping("/search")
    public ResponseEntity<Page<ConnectionLogDTO>> searchLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) Boolean successful,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        List<ActionLogType> actionTypes = parseActionTypes(types);

        Instant startDate = from != null
                ? from.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);

        Instant endDate = to != null
                ? to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now();

        Page<ConnectionLog> logs = connectionLogService.searchLogs(
                userId, ipAddress, actionTypes, successful, startDate, endDate, pageable);

        Page<ConnectionLogDTO> dtoPage = logs.map(ConnectionLogDTO::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Endpoint pour les administrateurs - statistiques système globales
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequiresDeviceTrustLevel(DeviceTrustLevel.HIGHLY_TRUSTED)
    @GetMapping("/system-stats")
    public ResponseEntity<Map<String, Object>> getSystemStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Instant startDate = from != null
                ? from.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);

        Instant endDate = to != null
                ? to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now();

        Map<String, Object> systemStats = connectionLogService.getSystemLoginStats(startDate, endDate);

        // Ajouter les statistiques géographiques
        Map<String, Long> locationStats = connectionLogService.getLoginsByLocation(startDate, endDate);
        systemStats.put("locationStats", locationStats);

        // Ajouter les statistiques journalières
        Map<LocalDate, Long> dailyStats = connectionLogService.getLoginsByDay(startDate, endDate);
        systemStats.put("dailyStats", dailyStats);

        return ResponseEntity.ok(systemStats);
    }

    /**
     * Endpoint pour obtenir les informations sur les types d'actions disponibles
     */
    @GetMapping("/action-types")
    public ResponseEntity<List<Map<String, String>>> getActionTypes() {
        List<Map<String, String>> actionTypesList = Arrays.stream(ActionLogType.values())
                .map(type -> {
                    Map<String, String> typeInfo = new HashMap<>();
                    typeInfo.put("key", type.name());
                    typeInfo.put("category", type.getCategory());
                    typeInfo.put("description", type.getDescription());
                    return typeInfo;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(actionTypesList);
    }

    /**
     * Convertit une liste de chaînes en liste d'énumérations ActionLogType
     */
    private List<ActionLogType> parseActionTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return null;
        }

        return types.stream()
                .map(type -> {
                    try {
                        return ActionLogType.valueOf(type);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}