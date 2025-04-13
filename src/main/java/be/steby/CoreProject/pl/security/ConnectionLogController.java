package be.steby.CoreProject.pl.security;

import be.steby.CoreProject.bll.services.ConnectionLogService;
import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.bll.services.security.SecurityService;
import be.steby.CoreProject.dl.entities.ConnectionLog;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.security.models.ConnectionLogDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable) {

        User currentUser = securityService.getAuthenticatedUser();
        Page<ConnectionLog> logs = connectionLogService.getUserConnectionHistory(currentUser, pageable);

        Page<ConnectionLogDTO> dtoPage = logs.map(ConnectionLogDTO::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Obtient les dernières connexions réussies de l'utilisateur courant
     */
    @GetMapping("/my-recent")
    public ResponseEntity<List<ConnectionLogDTO>> getMyRecentLogins() {
        User currentUser = securityService.getAuthenticatedUser();
        List<ConnectionLog> recentLogs = connectionLogService.getRecentLoginAttempts(currentUser);

        List<ConnectionLogDTO> dtoList = recentLogs.stream()
                .map(ConnectionLogDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    /**
     * Endpoint pour les administrateurs - obtient l'historique de connexion d'un utilisateur spécifique
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ConnectionLogDTO>> getUserConnectionHistory(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable) {

        User user = userService.getUserById(userId);
        Page<ConnectionLog> logs = connectionLogService.getUserConnectionHistory(user, pageable);

        Page<ConnectionLogDTO> dtoPage = logs.map(ConnectionLogDTO::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }
}