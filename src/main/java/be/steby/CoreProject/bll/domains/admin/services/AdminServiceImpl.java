package be.steby.CoreProject.bll.domains.admin.services;

import be.steby.CoreProject.bll.common.exceptions.UserPermissionExceptionFactory;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.models.reactivation.ReactivationEligibility;
import be.steby.CoreProject.bll.common.models.user.UserCreationRequest;
import be.steby.CoreProject.bll.common.models.user.UserCreationResult;
import be.steby.CoreProject.bll.common.services.context.RequestContextService;
import be.steby.CoreProject.bll.common.services.permissions.UserPermissionService;
import be.steby.CoreProject.bll.common.services.reactivation.ReactivationPolicyService;
import be.steby.CoreProject.bll.common.services.user.UserCreationService;
import be.steby.CoreProject.bll.common.services.mailer.MailerService;
import be.steby.CoreProject.bll.domains.admin.exceptions.AdminOperationException;
import be.steby.CoreProject.bll.domains.admin.exceptions.reactivation.InsufficientReactivationPermissionException;
import be.steby.CoreProject.bll.domains.admin.exceptions.reactivation.NeverReactivatableException;
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
 * ✅ SANS cycle de dépendances - utilise UserService pour l'auth et UserPermissionService pour les checks.
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

    private final UserPermissionService userPermissionService;
    private final AdminPolicyService adminPolicyService;
    private final ReactivationPolicyService reactivationPolicyService;

    // ===============================
    // GESTION DES UTILISATEURS
    // ===============================

    @Override
    @Transactional
    public User createUser(User user, HttpServletRequest request) {
        log.debug("Création utilisateur par admin - username: {}, roles: {}",
                user.getUsername(), user.getUserRoles());

        // 1. ✅ Validation authentification via UserService (pas de cycle)
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

        // 4. ✅ Validation via AdminPolicyService
        AdminValidationResult validation = adminPolicyService.validateUserCreation(adminRequest);
        if (!validation.isValid()) {
            String errorMessage = "Validation de création utilisateur échouée: " +
                    String.join(", ", validation.errors());
            log.warn(errorMessage);
            throw new AdminOperationException(errorMessage);
        }

        // 5. ✅ Vérification des permissions pour les rôles demandés (SANS cycle)
        User actor = userService.getAuthenticatedUser();
        for (UserRole role : user.getUserRoles()) {
            if (!userPermissionService.canGrantRole(actor, user, role)) {
                throw UserPermissionExceptionFactory.forInsufficientPermissions(
                        userPermissionService.getHighestRole(actor),
                        "attribuer le rôle " + role);
            }
        }

        // 6. Logique métier
        UserCreationRequest userCreationRequest = UserCreationRequest.forAdminCreate(user, requestContext);
        UserCreationResult result = userCreationService.createUser(userCreationRequest);

        log.info("Utilisateur créé avec succès par admin - ID: {}, username: {}",
                result.user().getId(), result.user().getUsername());

        return result.user();
    }

    @Override
    @Transactional
    public void deactivateUser(Long id, AdminDeactivationCategory deactivationCategory, String adminDeactivationDetails) {
        log.debug("Désactivation utilisateur par admin - targetId: {}, category: {}",
                id, deactivationCategory);

        // 1. ✅ Validation authentification via UserService
        userService.requireAdminPermissions();

        // 2. Récupération des acteurs
        User actor = userService.getAuthenticatedUser();
        User target = userService.getUserById(id);

        // 3. ✅ Vérification des permissions (SANS cycle)
        if (!userPermissionService.canDeactivateUser(actor, target)) {
            if (actor.getId().equals(id)) {
                throw UserPermissionExceptionFactory.forAdminSelfTargeting();
            } else {
                UserRole targetRole = userPermissionService.getHighestRole(target);
                throw UserPermissionExceptionFactory.forUnauthorizedUserAction("désactiver", targetRole);
            }
        }

        // 4. Validation des détails de désactivation
        AdminDeactivationRequest deactivationRequest = new AdminDeactivationRequest(
                id, deactivationCategory, adminDeactivationDetails);

        AdminValidationResult validationResult = adminPolicyService.validateDeactivationDetails(deactivationRequest);
        if (!validationResult.isValid()) {
            throw new AdminOperationException(
                    "Validation de désactivation échouée: " + String.join(", ", validationResult.errors()));
        }

        // 5. Logique métier
        userService.adminDeactivateUser(id, deactivationCategory, adminDeactivationDetails);

        log.info("Utilisateur désactivé avec succès - ID: {}, désactivé par: {}, catégorie: {}",
                id, actor.getUsername(), deactivationCategory);
    }

    @Override
    @Transactional
    public void activateUser(Long id) {
        log.debug("Admin activation request - targetId: {}", id);

        // 1. Authentication validation
        userService.requireAdminPermissions();

        // 2. Get actors
        User currentAdmin  = userService.getAuthenticatedUser();
        User targetUser = userService.getUserById(id);

        // 3. ✅ SIMPLE VALIDATION: Direct enum usage
        if (targetUser.isAdminDeactivated()) {
            ReactivationEligibility eligibility = reactivationPolicyService.checkEligibility(targetUser, currentAdmin);

            if (!eligibility.isEligible()) {
                // Gestion spécifique des erreurs selon le type
                switch (eligibility.getType()) {
                    case NEVER_REACTIVATABLE ->
                            throw new NeverReactivatableException(eligibility.getReason());
                    case INSUFFICIENT_PERMISSIONS ->
                            throw new InsufficientReactivationPermissionException(eligibility.getReason());
                    default ->
                            throw new AdminOperationException("Reactivation denied: " + eligibility.getReason());
                }
            }}

        userService.adminActivateUser(targetUser, currentAdmin);


        log.info("User successfully activated by admin - ID: {}, activated by: {}", id, currentAdmin.getUsername());
    }
    // ===============================
    // GESTION DES RÔLES
    // ===============================

    @Override
    @Transactional
    public void grantUserRole(Long id, UserRole role) {
        log.debug("Attribution rôle par admin - targetId: {}, role: {}", id, role);

        // 1. ✅ Validation authentification via UserService
        userService.requireAdminPermissions();

        // 2. Récupération des acteurs
        User actor = userService.getAuthenticatedUser();
        User target = userService.getUserById(id);

        // 3. ✅ Validation des permissions (SANS cycle)
        if (!userPermissionService.canGrantRole(actor, target, role)) {
            UserRole actorRole = userPermissionService.getHighestRole(actor);
            if (role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN) {
                throw UserPermissionExceptionFactory.forInsufficientPermissions(
                        UserRole.SUPER_ADMIN, "attribuer le rôle " + role);
            } else {
                throw UserPermissionExceptionFactory.forInsufficientPermissions(
                        UserRole.ADMIN, "attribuer le rôle " + role);
            }
        }

        // 4. Logique métier
        userService.grantUserRole(id, role);

        log.info("Rôle {} attribué à l'utilisateur {} par {}",
                role, target.getUsername(), actor.getUsername());
    }

    @Override
    @Transactional
    public void revokeUserRole(Long id, UserRole role) {
        log.debug("Révocation rôle par admin - targetId: {}, role: {}", id, role);

        // 1. ✅ Validation authentification via UserService
        userService.requireAdminPermissions();

        // 2. Récupération des acteurs
        User actor = userService.getAuthenticatedUser();
        User target = userService.getUserById(id);

        // 3. ✅ Validation des permissions (SANS cycle)
        if (!userPermissionService.canRevokeRole(actor, target, role)) {
            throw UserPermissionExceptionFactory.forInsufficientPermissions(
                    userPermissionService.getHighestRole(actor), "révoquer le rôle " + role);
        }

        // 4. Logique métier
        userService.revokeUserRole(id, role);

        log.info("Rôle {} révoqué de l'utilisateur {} par {}",
                role, target.getUsername(), actor.getUsername());
    }

    // ===============================
    // AUTRES OPÉRATIONS ADMIN (inchangées)
    // ===============================

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userService.requireSuperAdminPermissions(); // ✅ Via UserService
        userService.deleteUser(id);
    }

    @Override
    @Transactional
    public void gdprUserDelete(User user) {
        userService.requireSuperAdminPermissions(); // ✅ Via UserService
        userService.gdprUserDelete(user);
    }

    @Override
    @Transactional
    public void triggerPasswordReset(Long id) {
        userService.requireAdminPermissions(); // ✅ Via UserService

        User target = getUserById(id);
        PasswordResetToken token = passwordResetTokenService.createPasswordResetToken(target);
        mailerService.sendPasswordReset(token.getToken(), target);
    }

    @Override
    public Page<User> searchUsers(String query, Pageable pageable) {
        userService.requireAdminPermissions(); // ✅ Via UserService
        return userService.searchUsers(query, pageable);
    }

    @Override
    public Page<User> searchUsersByCriteria(String username, String firstname, String lastname,
                                            String email, String phoneNumber, Pageable pageable) {
        userService.requireAdminPermissions(); // ✅ Via UserService
        return userService.searchUsersByCriteria(username, firstname, lastname, email, phoneNumber, pageable);
    }

    @Override
    public User getUserById(Long id) {
        userService.requireAdminPermissions(); // ✅ Via UserService
        return userService.getUserById(id);
    }

    @Override
    public List<Device> getUserDevices(Long id) {
        userService.requireAdminPermissions(); // ✅ Via UserService
        User user = userService.getUserById(id);
        return deviceService.getUserDevice(user);
    }

    @Override
    public Long getTotalUsers() {
        userService.requireAdminPermissions(); // ✅ Via UserService
        return userService.getTotalUsers();
    }
}