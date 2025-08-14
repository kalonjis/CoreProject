package be.steby.CoreProject.bll.domains.admin.services;

import be.steby.CoreProject.bll.common.exceptions.UserPermissionExceptionFactory;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.models.user.UserCreationRequest;
import be.steby.CoreProject.bll.common.models.user.UserCreationResult;
import be.steby.CoreProject.bll.common.services.context.RequestContextService;
import be.steby.CoreProject.bll.common.services.permissions.UserPermissionService;
import be.steby.CoreProject.bll.common.services.user.UserCreationService;
import be.steby.CoreProject.bll.common.services.mailer.MailerService;
import be.steby.CoreProject.bll.domains.admin.exceptions.AdminOperationException;
import be.steby.CoreProject.bll.domains.admin.models.AdminDeactivationRequest;
import be.steby.CoreProject.bll.domains.admin.models.AdminUserCreationRequest;
import be.steby.CoreProject.bll.domains.admin.models.AdminValidationResult;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.password.services.tokens.PasswordResetTokenServiceImpl;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import be.steby.CoreProject.dl.enums.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implémentation complète du service d'administration utilisateur.
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserService userService;
    private final DeviceService deviceService;
    private final MailerService mailerService;
    private final PasswordResetTokenServiceImpl passwordResetTokenService;
    private final RequestContextService requestContextService;
    private final UserCreationService userCreationService;

    // ✅ Garde les deux : le meilleur des deux mondes
    private final UserPermissionService userPermissionService;
    private final AdminPolicyService adminPolicyService;

    /**
     * Crée un nouvel utilisateur avec validation complète via AdminPolicyService.
     * Ici AdminPolicyService apporte une vraie valeur ajoutée (email, rôles, etc.)
     */
    @Override
    @Transactional
    public User createUser(User user, HttpServletRequest request) {
        log.debug("Création utilisateur par admin - username: {}, roles: {}",
                user.getUsername(), user.getUserRoles());

        // 1. Validation authentification
        userService.requireAdminPermissions();

        // 2. Capture contexte
        RequestContext requestContext = requestContextService.captureRequestContext(request);

        // 3. Conversion vers modèle admin
        AdminUserCreationRequest adminRequest = new AdminUserCreationRequest(
                user.getUsername(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getUserRoles(),
                true,
                requestContext
        );

        // 4. ✅ GARDE AdminPolicyService ici - validation complexe justifiée
        AdminValidationResult validation = adminPolicyService.validateUserCreation(adminRequest);
        if (!validation.isValid()) {
            String errorMessage = "Validation de création utilisateur échouée: " +
                    String.join(", ", validation.errors());
            log.warn(errorMessage);
            throw new AdminOperationException(errorMessage);
        }

        // 5. Logique métier
        UserCreationRequest userCreationRequest = UserCreationRequest.forAdminCreate(user, requestContext);
        UserCreationResult result = userCreationService.createUser(userCreationRequest);

        log.info("Utilisateur créé avec succès par admin - ID: {}, username: {}",
                result.user().getId(), result.user().getUsername());

        return result.user();
    }

    /**
     * Désactive un utilisateur - RETOUR à UserPermissionService pour plus de précision.
     * AdminPolicyService n'apportait pas de valeur ajoutée ici, juste de la duplication.
     */
    @Override
    @Transactional
    public void deactivateUser(Long id, AdminDeactivationCategory deactivationCategory, String adminDeactivationDetails) {
        log.debug("Désactivation utilisateur par admin - targetId: {}, category: {}",
                id, deactivationCategory);

        // 1. Validation authentification
        userService.requireAdminPermissions();

        // 2. Récupération des acteurs
        User actor = userService.getAuthenticatedUser();
        User target = userService.getUserById(id);

        // 3. ✅ RETOUR à UserPermissionService - plus précis et sans duplication
        if (!userPermissionService.canDeactivateUser(actor, target)) {
            if (actor.getId().equals(id)) {
                UserRole actorRole = userPermissionService.getHighestRole(actor);
                throw UserPermissionExceptionFactory.forSelfDeactivationByRole(actorRole);
            } else {
                UserRole targetRole = userPermissionService.getHighestRole(target);
                throw UserPermissionExceptionFactory.forUnauthorizedUserAction("désactiver", targetRole);
            }
        }

        // 4. ✅ SEULE validation AdminPolicyService - pour les détails de désactivation
        AdminDeactivationRequest deactivationRequest = new AdminDeactivationRequest(
                id, deactivationCategory, adminDeactivationDetails);

        AdminValidationResult detailsValidation = adminPolicyService.validateDeactivationDetails(deactivationRequest);
        if (!detailsValidation.isValid()) {
            String errorMessage = "Validation des détails de désactivation échouée: " +
                    String.join(", ", detailsValidation.errors());
            log.warn(errorMessage);
            throw new AdminOperationException(errorMessage);
        }

        // 5. Logique métier
        userService.adminDeactivateUser(id, deactivationCategory, adminDeactivationDetails);

        log.info("Utilisateur désactivé avec succès - ID: {}, catégorie: {}, désactivé par: {}",
                id, deactivationCategory, actor.getUsername());
    }

    /**
     * Attribution de rôle - RETOUR à UserPermissionService pour la précision.
     */
    @Override
    @Transactional
    public void grantUserRole(Long id, UserRole role) {
        log.debug("Attribution rôle par admin - targetId: {}, role: {}", id, role);

        // 1. Validation authentification
        userService.requireAdminPermissions();

        // 2. Récupération des acteurs
        User actor = userService.getAuthenticatedUser();
        User target = userService.getUserById(id);

        // 3. ✅ RETOUR à UserPermissionService - messages plus précis
        if (!userPermissionService.canGrantRole(actor, target, role)) {
            if (actor.getId().equals(id)) {
                throw UserPermissionExceptionFactory.forSelfRoleManagement("accorder des rôles à");
            } else if (role == UserRole.SUPER_ADMIN) {
                throw UserPermissionExceptionFactory.forSuperAdminRoleGrant();
            } else {
                throw UserPermissionExceptionFactory.forInsufficientPermissions(UserRole.ADMIN, "accorder ce rôle");
            }
        }

        // 4. Logique métier
        userService.grantUserRole(id, role);

        log.info("Rôle accordé avec succès - userId: {}, role: {}, accordé par: {}",
                id, role, actor.getUsername());
    }

    /**
     * Révocation de rôle - RETOUR à UserPermissionService pour la précision.
     */
    @Override
    @Transactional
    public void revokeUserRole(Long id, UserRole role) {
        log.debug("Révocation rôle par admin - targetId: {}, role: {}", id, role);

        // 1. Validation authentification
        userService.requireAdminPermissions();

        // 2. Récupération des acteurs
        User actor = userService.getAuthenticatedUser();
        User target = userService.getUserById(id);

        // 3. ✅ RETOUR à UserPermissionService - messages plus précis
        if (!userPermissionService.canRevokeRole(actor, target, role)) {
            if (actor.getId().equals(id)) {
                throw UserPermissionExceptionFactory.forSelfRoleManagement("révoquer des rôles de");
            } else if (role == UserRole.SUPER_ADMIN) {
                throw UserPermissionExceptionFactory.forSuperAdminRoleRevoke();
            } else {
                throw UserPermissionExceptionFactory.forInsufficientPermissions(UserRole.ADMIN, "révoquer ce rôle");
            }
        }

        // 4. Logique métier
        userService.revokeUserRole(id, role);

        log.info("Rôle révoqué avec succès - userId: {}, role: {}, révoqué par: {}",
                id, role, actor.getUsername());
    }

    /**
     * Activation utilisateur - UserPermissionService suffit.
     */
    @Override
    @Transactional
    public void activateUser(Long id) {
        log.debug("Activation utilisateur par admin - targetId: {}", id);

        // 1. Validation authentification
        userService.requireAdminPermissions();

        // 2. Récupération des acteurs
        User actor = userService.getAuthenticatedUser();
        User target = userService.getUserById(id);

        // 3. ✅ UserPermissionService directement - simple et précis
        if (!userPermissionService.canActivateUser(actor, target)) {
            if (actor.getId().equals(id)) {
                UserRole actorRole = userPermissionService.getHighestRole(actor);
                throw UserPermissionExceptionFactory.forSelfActivationByRole(actorRole);
            } else {
                UserRole targetRole = userPermissionService.getHighestRole(target);
                throw UserPermissionExceptionFactory.forUnauthorizedUserAction("activer", targetRole);
            }
        }

        // 4. Logique métier
        userService.activateUser(id);

        log.info("Utilisateur activé avec succès - ID: {}, activé par: {}",
                id, actor.getUsername());
    }

    // ===============================
    // Autres méthodes gardent le même pattern...
    // ===============================

    @Override
    @Transactional
    public void gdprUserDelete(User user) {
        userService.requireAdminPermissions();
        userService.gdprUserDelete(user);
    }

    @Override
    @Transactional
    public void triggerPasswordReset(Long id) {
        userService.requireAdminPermissions();

        User target = getUserById(id);
        PasswordResetToken token = passwordResetTokenService.createPasswordResetToken(target);
        mailerService.sendPasswordReset(token.getToken(), target);
    }

    @Override
    public Page<User> searchUsers(String query, Pageable pageable) {
        userService.requireAdminPermissions();
        return userService.searchUsers(query, pageable);
    }

    @Override
    public Page<User> searchUsersByCriteria(String username, String firstname, String lastname,
                                            String email, String phoneNumber, Pageable pageable) {
        userService.requireAdminPermissions();
        return userService.searchUsersByCriteria(username, firstname, lastname, email, phoneNumber, pageable);
    }

    @Override
    public User getUserById(Long id) {
        userService.requireAdminPermissions();
        return userService.getUserById(id);
    }

    @Override
    public List<Device> getUserDevices(Long id) {
        userService.requireAdminPermissions();
        User user = userService.getUserById(id);
        return deviceService.getUserDevice(user);
    }

    @Override
    public Long getTotalUsers() {
        userService.requireAdminPermissions();
        return userService.getTotalUsers();
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userService.requireAdminPermissions();
        userService.deleteUser(id);
    }
}