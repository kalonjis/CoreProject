package be.steby.CoreProject.pl.controllers.admin;

import be.steby.CoreProject.bll.domains.admin.services.AdminService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import be.steby.CoreProject.dl.enums.admin.deactivation.DeactivationMainCategory;
import be.steby.CoreProject.pl.models.admin.UserDeactivationForm;
import be.steby.CoreProject.pl.models.admin.UserRegisterForm;
import be.steby.CoreProject.pl.models.admin.UserRoleForm;
import be.steby.CoreProject.pl.models.user.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;


/**
 * Contrôleur admin amélioré selon le pattern du projet.
 * Suit le modèle de PasswordController : léger, conversion propre, réponses informatives.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping("/api/admin/users")
@Slf4j
public class AdminUserController {

    private final AdminService adminService;
    private final UserService userService;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final PagedResourcesAssembler<User> pagedResourcesAssembler;

    /**
     * Endpoint de désactivation amélioré selon pattern projet.
     * Suit le modèle de PasswordController.changePassword()
     *
     * @param id ID de l'utilisateur à désactiver
     * @param form Formulaire avec validation (includes nouvelles propriétés)
     * @param request HttpServletRequest pour contexte (pattern projet)
     * @return ResponseEntity avec message informatif (pattern projet)
     */
    @PatchMapping("/deactivate/{id}")
    public ResponseEntity<Map<String, String>> deactivateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDeactivationForm form,
            HttpServletRequest request) {

        log.info("Demande de désactivation utilisateur ID: {} reçue", id);

        // Appel service - Simple et direct
        adminService.deactivateUser(
                id,
                form.deactivationCategory(),
                form.adminDeactivationDetails(),
                request
        );

        // Réponse informative selon pattern projet (comme PasswordController)
        Map<String, String> response = new HashMap<>();
        response.put("message", "Utilisateur désactivé avec succès");
        response.put("userId", id.toString());
        response.put("category", form.deactivationCategory().name());

        log.info("Désactivation utilisateur ID: {} réussie", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint d'activation selon pattern projet.
     */
    @PatchMapping("/activate/{id}")
    public ResponseEntity<Map<String, String>> activateUser(
            @PathVariable Long id,
            HttpServletRequest request) {

        log.info("Demande d'activation utilisateur ID: {} reçue", id);

        adminService.activateUser(id, request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Utilisateur activé avec succès");
        response.put("userId", id.toString());

        log.info("Activation utilisateur ID: {} réussie", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint d'attribution de rôle selon pattern projet.
     */
    @PatchMapping("/grant-role/{id}")
    public ResponseEntity<Map<String, String>> grantUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleForm form,
            HttpServletRequest request) {

        log.info("Attribution rôle {} à l'utilisateur ID: {}", form.userRole(), id);

        adminService.grantUserRole(id, form.userRole());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Rôle attribué avec succès");
        response.put("userId", id.toString());
        response.put("role", form.userRole().name());

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de révocation de rôle selon pattern projet.
     */
    @PatchMapping("/revoke-role/{id}")
    public ResponseEntity<Map<String, String>> revokeUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleForm form,
            HttpServletRequest request) {

        log.info("Révocation rôle {} pour l'utilisateur ID: {}", form.userRole(), id);

        adminService.revokeUserRole(id, form.userRole());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Rôle révoqué avec succès");
        response.put("userId", id.toString());
        response.put("role", form.userRole().name());

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de réinitialisation forcée de mot de passe selon pattern projet.
     */
    @PatchMapping("/force-reset-password/{id}")
    public ResponseEntity<Map<String, String>> forceResetPassword(
            @PathVariable Long id,
            HttpServletRequest request) {

        log.info("Réinitialisation forcée mot de passe pour utilisateur ID: {}", id);

        adminService.triggerPasswordReset(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Un email de réinitialisation de mot de passe a été envoyé à l'utilisateur");
        response.put("userId", id.toString());

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de récupération d'utilisateur par ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        User user = adminService.getUserById(id);
        return ResponseEntity.ok(UserDTO.fromEntity(user));
    }

    /**
     * Endpoint de suppression d'utilisateur selon pattern projet.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(
            @PathVariable Long id,
            HttpServletRequest request) {

        log.warn("Demande de suppression utilisateur ID: {} reçue", id);

        adminService.deleteUser(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Utilisateur supprimé avec succès");
        response.put("userId", id.toString());

        log.warn("Suppression utilisateur ID: {} réalisée", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de récupération des appareils d'un utilisateur.
     */
    @GetMapping("/{id}/devices")
    public ResponseEntity<?> getUserDevices(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserDevices(id));
    }

    /**
     * Endpoint de recherche d'utilisateurs.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<User>> searchUsers(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<User> users;
        if (query != null && !query.trim().isEmpty()) {
            users = adminService.searchUsers(query, pageable);
        } else {
            users = adminService.searchUsers(null, pageable);
        }

        return ResponseEntity.ok(users);
    }

    /**
     * Endpoint de recherche d'utilisateurs par critères.
     */
    @GetMapping("/searchbycriteria")
    public ResponseEntity<Page<User>> searchUsersByCriteria(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String firstname,
            @RequestParam(required = false) String lastname,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<User> users = adminService.searchUsersByCriteria(
                username, firstname, lastname, email, phoneNumber, pageable);

        return ResponseEntity.ok(users);
    }

    /**
     * Endpoint de liste de tous les utilisateurs.
     */
    @GetMapping("/all")
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {

        Pageable pageable = getPageable(page, size, sort);
        Page<User> users = adminService.searchUsers(null, pageable);

        return ResponseEntity.ok(users);
    }

    /**
     * Endpoint de création d'utilisateur.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> createUser(
            @Valid @RequestBody UserRegisterForm form,
            HttpServletRequest request) {

        log.info("Création d'utilisateur par admin: {}", form.username());

        User user = form.toEntity();
        User createdUser = adminService.createUser(user, request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Utilisateur créé avec succès");
        response.put("userId", createdUser.getId().toString());
        response.put("username", createdUser.getUsername());

        return ResponseEntity.created(
                URI.create("/api/admin/users/" + createdUser.getId())
        ).body(response);
    }

    /**
     * Endpoint d'informations sur les statistiques admin.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", adminService.getTotalUsers());
        // TODO: Ajouter d'autres statistiques selon besoins

        return ResponseEntity.ok(stats);
    }

    /**
     * Endpoint pour obtenir les informations de désactivation disponibles.
     * Utilise la nouvelle structure catégories/sous-catégories.
     */
    @GetMapping("/deactivation/info")
    public ResponseEntity<Map<String, Object>> getDeactivationInfo() {
        Map<String, Object> info = new HashMap<>();

        // Informations sur les catégories principales
        info.put("mainCategories", java.util.Arrays.stream(
                        DeactivationMainCategory.values())
                .map(mainCat -> Map.of(
                        "value", mainCat.name(),
                        "displayName", mainCat.getDisplayName(),
                        "description", mainCat.getDescription()
                ))
                .toList());

        // Informations sur toutes les sous-catégories
        info.put("categories", java.util.Arrays.stream(
                        AdminDeactivationCategory.values())
                .map(cat -> Map.of(
                        "value", cat.name(),
                        "displayName", cat.getDisplayName(),
                        "description", cat.getDescription(),
                        "mainCategory", cat.getMainCategory().name(),
                        "severityLevel", cat.getSeverityLevel(),
                        "allowsReactivation", cat.allowsReactivation(),
                        "requiresSuperAdmin", cat.requiresSuperAdminApproval(),
                        "invalidatesSessions", cat.requiresSessionInvalidation()
                ))
                .toList());

        // Contraintes de validation simplifiées
        info.put("validation", Map.of(
                "detailsMinLength", 10,
                "detailsMaxLength", 500
        ));

        return ResponseEntity.ok(info);
    }

    /**
     * Méthode utilitaire pour créer Pageable.
     */
    private Pageable getPageable(int page, int size, String sort) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = (sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1]))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Sort sorting = Sort.by(direction, sortParams[0]);
        return PageRequest.of(page, size, sorting);
    }
}