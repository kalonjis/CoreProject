package be.steby.CoreProject.pl.security;

import be.steby.CoreProject.bll.services.ActivityLogService;
import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.ActionLogType;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import be.steby.CoreProject.il.device.RequiresDeviceTrustLevel;
import be.steby.CoreProject.pl.assemblers.ConnectionLogModelAssembler;
import be.steby.CoreProject.pl.security.models.ConnectionLogDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Contrôleur pour gérer les journaux de connexion et d'activité des utilisateurs
 */
@RestController
@RequestMapping("/api/security/logs")
@RequiredArgsConstructor
public class ConnectionLogController {

    private final ActivityLogService activityLogService;
    private final UserService userService;
    private final PagedResourcesAssembler<ActivityLog> pagedResourcesAssembler;
    private final ConnectionLogModelAssembler logAssembler;

    /**
     * Obtient l'historique de connexion de l'utilisateur courant
     */
    @GetMapping("/my-history")
    public ResponseEntity<PagedModel<EntityModel<ConnectionLogDTO>>> getMyConnectionHistory(
            @PageableDefault(size = 20, sort = "timestamp", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {

        User currentUser = userService.getAuthenticatedUser();
        Page<ActivityLog> logs = activityLogService.getUserConnectionHistory(currentUser, pageable);

        PagedModel<EntityModel<ConnectionLogDTO>> pagedModel = pagedResourcesAssembler.toModel(
                logs,
                logAssembler::toModel);

        // Add self link to the paged model
        pagedModel.add(linkTo(methodOn(ConnectionLogController.class)
                .getMyConnectionHistory(pageable)).withSelfRel());

        return ResponseEntity.ok(pagedModel);
    }

    /**
     * Obtient les dernières connexions réussies de l'utilisateur courant
     */
    @GetMapping("/my-recent-logins")
    public ResponseEntity<CollectionModel<EntityModel<ConnectionLogDTO>>> getMyRecentLogins() {
        User currentUser = userService.getAuthenticatedUser();
        List<ActivityLog> recentLogs = activityLogService.getRecentLoginAttempts(currentUser);

        List<EntityModel<ConnectionLogDTO>> dtoList = recentLogs.stream()
                .map(logAssembler::toModel)
                .collect(Collectors.toList());

        CollectionModel<EntityModel<ConnectionLogDTO>> collectionModel = CollectionModel.of(
                dtoList,
                linkTo(methodOn(ConnectionLogController.class).getMyRecentLogins()).withSelfRel(),
                linkTo(methodOn(ConnectionLogController.class).getMyConnectionHistory(null)).withRel("history")
        );

        return ResponseEntity.ok(collectionModel);
    }

    /**
     * Recherche les actions de l'utilisateur courant par type
     */
    @GetMapping("/my-actions")
    public ResponseEntity<PagedModel<EntityModel<ConnectionLogDTO>>> getMyActions(
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "timestamp", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {

        User currentUser = userService.getAuthenticatedUser();
        List<ActionLogType> actionTypes = parseActionTypes(types);

        Instant startDate = from != null
                ? from.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);

        Instant endDate = to != null
                ? to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now();

        Page<ActivityLog> logs = activityLogService.getUserActionHistory(
                currentUser, actionTypes, startDate, endDate, pageable);

        PagedModel<EntityModel<ConnectionLogDTO>> pagedModel = pagedResourcesAssembler.toModel(
                logs,
                logAssembler::toModel);

        // Add self link to the paged model
        pagedModel.add(linkTo(methodOn(ConnectionLogController.class)
                .getMyActions(types, from, to, pageable)).withSelfRel());

        return ResponseEntity.ok(pagedModel);
    }

    /**
     * Obtient des statistiques sur l'activité de l'utilisateur courant
     */
    @GetMapping("/my-stats")
    public ResponseEntity<Map<String, Object>> getMyActivityStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        User currentUser = userService.getAuthenticatedUser();

        Instant startDate = from != null
                ? from.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);

        Instant endDate = to != null
                ? to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now();

        Map<String, Object> stats = activityLogService.getUserActivityStats(
                currentUser, startDate, endDate);

        return ResponseEntity.ok(stats);
    }

    /**
     * Vérifie s'il y a des activités suspectes pour l'utilisateur courant
     */
    @GetMapping("/my-security-alerts")
    public ResponseEntity<CollectionModel<EntityModel<ConnectionLogDTO>>> getMySecurityAlerts() {
        User currentUser = userService.getAuthenticatedUser();
        List<ActivityLog> suspiciousLogs = activityLogService.detectSuspiciousActivity(currentUser);

        List<EntityModel<ConnectionLogDTO>> dtoList = suspiciousLogs.stream()
                .map(logAssembler::toModel)
                .collect(Collectors.toList());

        CollectionModel<EntityModel<ConnectionLogDTO>> collectionModel = CollectionModel.of(
                dtoList,
                linkTo(methodOn(ConnectionLogController.class).getMySecurityAlerts()).withSelfRel(),
                linkTo(methodOn(ConnectionLogController.class).getMyConnectionHistory(null)).withRel("history")
        );

        return ResponseEntity.ok(collectionModel);
    }

    /**
     * Endpoint pour les administrateurs - obtient l'historique de connexion d'un utilisateur spécifique
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequiresDeviceTrustLevel(DeviceTrustLevel.TRUSTED)
    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedModel<EntityModel<ConnectionLogDTO>>> getUserConnectionHistory(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "timestamp", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {

        User user = userService.getUserById(userId);
        Page<ActivityLog> logs = activityLogService.getUserConnectionHistory(user, pageable);

        PagedModel<EntityModel<ConnectionLogDTO>> pagedModel = pagedResourcesAssembler.toModel(
                logs,
                log -> EntityModel.of(ConnectionLogDTO.fromEntity(log)));

        // Add self link to the paged model
        pagedModel.add(linkTo(methodOn(ConnectionLogController.class)
                .getUserConnectionHistory(userId, pageable)).withSelfRel());

        return ResponseEntity.ok(pagedModel);
    }

    /**
     * Endpoint pour les administrateurs - obtient les actions d'un utilisateur spécifique par type
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequiresDeviceTrustLevel(DeviceTrustLevel.TRUSTED)
    @GetMapping("/user/{userId}/actions")
    public ResponseEntity<PagedModel<EntityModel<ConnectionLogDTO>>> getUserActions(
            @PathVariable Long userId,
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "timestamp", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {

        User user = userService.getUserById(userId);
        List<ActionLogType> actionTypes = parseActionTypes(types);

        Instant startDate = from != null
                ? from.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);

        Instant endDate = to != null
                ? to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now();

        Page<ActivityLog> logs = activityLogService.getUserActionHistory(
                user, actionTypes, startDate, endDate, pageable);

        PagedModel<EntityModel<ConnectionLogDTO>> pagedModel = pagedResourcesAssembler.toModel(
                logs,
                log -> EntityModel.of(ConnectionLogDTO.fromEntity(log)));

        // Add self link to the paged model
        pagedModel.add(linkTo(methodOn(ConnectionLogController.class)
                .getUserActions(userId, types, from, to, pageable)).withSelfRel());

        return ResponseEntity.ok(pagedModel);
    }

    /**
     * Endpoint pour les administrateurs - recherche avancée des logs
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequiresDeviceTrustLevel(DeviceTrustLevel.HIGHLY_TRUSTED)
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<ConnectionLogDTO>>> searchLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) Boolean successful,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "timestamp", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {

        List<ActionLogType> actionTypes = parseActionTypes(types);

        Instant startDate = from != null
                ? from.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);

        Instant endDate = to != null
                ? to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now();

        Page<ActivityLog> logs = activityLogService.searchLogs(
                userId, ipAddress, actionTypes, successful, startDate, endDate, pageable);

        PagedModel<EntityModel<ConnectionLogDTO>> pagedModel = pagedResourcesAssembler.toModel(
                logs,
                log -> EntityModel.of(ConnectionLogDTO.fromEntity(log)));

        // Add self link to the paged model
        pagedModel.add(linkTo(methodOn(ConnectionLogController.class)
                .searchLogs(userId, ipAddress, types, successful, from, to, pageable)).withSelfRel());

        return ResponseEntity.ok(pagedModel);
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

        Map<String, Object> systemStats = activityLogService.getSystemLoginStats(startDate, endDate);

        // Ajouter les statistiques géographiques
        Map<String, Long> locationStats = activityLogService.getLoginsByLocation(startDate, endDate);
        systemStats.put("locationStats", locationStats);

        // Ajouter les statistiques journalières
        Map<LocalDate, Long> dailyStats = activityLogService.getLoginsByDay(startDate, endDate);
        systemStats.put("dailyStats", dailyStats);

        return ResponseEntity.ok(systemStats);
    }

    /**
     * Endpoint pour obtenir les informations sur les types d'actions disponibles
     */
    @GetMapping("/action-types")
    public ResponseEntity<CollectionModel<EntityModel<Map<String, String>>>> getActionTypes() {
        List<EntityModel<Map<String, String>>> actionTypesList = Arrays.stream(ActionLogType.values())
                .map(type -> {
                    Map<String, String> typeInfo = new HashMap<>();
                    typeInfo.put("key", type.name());
                    typeInfo.put("category", type.getCategory());
                    typeInfo.put("description", type.getDescription());
                    return EntityModel.of(typeInfo);
                })
                .collect(Collectors.toList());

        CollectionModel<EntityModel<Map<String, String>>> collectionModel = CollectionModel.of(
                actionTypesList,
                linkTo(methodOn(ConnectionLogController.class).getActionTypes()).withSelfRel()
        );

        return ResponseEntity.ok(collectionModel);
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