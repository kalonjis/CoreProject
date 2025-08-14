package be.steby.CoreProject.bll.domains.admin.services;

import be.steby.CoreProject.bll.common.exceptions.UserPermissionExceptionFactory;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.models.user.UserCreationRequest;
import be.steby.CoreProject.bll.common.models.user.UserCreationResult;
import be.steby.CoreProject.bll.common.services.context.RequestContextService;
import be.steby.CoreProject.bll.common.services.permissions.UserPermissionService;
import be.steby.CoreProject.bll.common.services.user.UserCreationService;
import be.steby.CoreProject.bll.common.services.mailer.MailerService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;


@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserService userService;
    private final DeviceService deviceService;
    private final MailerService mailerService;
    private final PasswordResetTokenServiceImpl passwordResetTokenService;
    private final RequestContextService requestContextService;
    private final UserCreationService userCreationService;
    private final UserPermissionService userPermissionService;


    @Override
    public User createUser(User user, HttpServletRequest request) {
        // ✅ Validation simple et centralisée
        userService.requireAdminPermissions();

        // ✅ Validation spécifique pour SUPER_ADMIN si nécessaire
        boolean userHasSuperAdmin = user.getUserRoles().contains(UserRole.SUPER_ADMIN);
        if (userHasSuperAdmin) {
            userService.requireSuperAdminPermissions();
        }

        RequestContext requestContext = requestContextService.captureRequestContext(request);
        UserCreationRequest userCreationRequest = UserCreationRequest.forAdminCreate(user, requestContext);
        UserCreationResult result = userCreationService.createUser(userCreationRequest);
        return result.user();
    }

    /**
     * @param id
     */
    @Override
    public void deleteUser(Long id) {

    }


    @Override
    public void activateUser(Long id) {
        if (!userService.authenticatedHasRole(UserRole.ADMIN)){
            throw UserPermissionExceptionFactory.forAdminPermissionRequired("activer un utilisateur");
        }

        User actor = userService.getAuthenticatedUser();
        User target = userService.getUserById(id);

        if (!userPermissionService.canActivateUser(actor, target)) {
            if (actor.getId().equals(id)) {
                // ✅ FIX - Message adapté au rôle spécifique de l'acteur
                UserRole actorRole = userPermissionService.getHighestRole(actor);
                throw UserPermissionExceptionFactory.forSelfActivationByRole(actorRole);
            } else {
                UserRole targetRole = userPermissionService.getHighestRole(target);
                throw UserPermissionExceptionFactory.forUnauthorizedUserAction("activer", targetRole);
            }
        }

        userService.activateUser(id);
    }


    @Override
    public void deactivateUser(Long id, AdminDeactivationCategory deactivationCategory, String adminDeactivationDetails) {
        if (!userService.authenticatedHasRole(UserRole.ADMIN)){
            throw UserPermissionExceptionFactory.forAdminPermissionRequired("désactiver un utilisateur");
        }

        User actor = userService.getAuthenticatedUser();
        User target = userService.getUserById(id);

        if (!userPermissionService.canDeactivateUser(actor, target)) {
            if (actor.getId().equals(id)) {
                // ✅ FIX - Message adapté au rôle spécifique de l'acteur
                UserRole actorRole = userPermissionService.getHighestRole(actor);
                throw UserPermissionExceptionFactory.forSelfDeactivationByRole(actorRole);
            } else {
                UserRole targetRole = userPermissionService.getHighestRole(target);
                throw UserPermissionExceptionFactory.forUnauthorizedUserAction("désactiver", targetRole);
            }
        }

        userService.adminDeactivateUser(id, deactivationCategory, adminDeactivationDetails);
    }

    @Override
    public void gdprUserDelete(User user) {
        if (!userService.authenticatedHasRole(UserRole.ADMIN)){
            throw UserPermissionExceptionFactory.forAdminPermissionRequired("supprimer un utilisateur");
        }
        userService.gdprUserDelete(user);
    }


    @Override
    public void grantUserRole(Long id, UserRole role) {
        if (!userService.authenticatedHasRole(UserRole.ADMIN)){
            throw UserPermissionExceptionFactory.forAdminPermissionRequired("accorder des rôles");
        }

        User actor = userService.getAuthenticatedUser();
        User target = userService.getUserById(id);

        // ✅ Utilisation du nouveau service pour vérifier les permissions
        if (!userPermissionService.canGrantRole(actor, target, role)) {
            if (actor.getId().equals(id)) {
                throw UserPermissionExceptionFactory.forSelfRoleManagement("accorder des rôles à");
            } else if (role == UserRole.SUPER_ADMIN) {
                throw UserPermissionExceptionFactory.forSuperAdminRoleGrant();
            } else {
                throw UserPermissionExceptionFactory.forInsufficientPermissions(UserRole.ADMIN, "accorder ce rôle");
            }
        }

        userService.grantUserRole(id, role);
    }

    @Override
    public void revokeUserRole(Long id, UserRole role) {
        if (!userService.authenticatedHasRole(UserRole.ADMIN)){
            throw UserPermissionExceptionFactory.forAdminPermissionRequired("révoquer des rôles");
        }

        User actor = userService.getAuthenticatedUser();
        User target = userService.getUserById(id);

        // ✅ Utilisation du nouveau service pour vérifier les permissions
        if (!userPermissionService.canRevokeRole(actor, target, role)) {
            if (actor.getId().equals(id)) {
                throw UserPermissionExceptionFactory.forSelfRoleManagement("révoquer des rôles de");
            } else if (role == UserRole.SUPER_ADMIN) {
                throw UserPermissionExceptionFactory.forSuperAdminRoleRevoke();
            } else {
                throw UserPermissionExceptionFactory.forInsufficientPermissions(UserRole.ADMIN, "révoquer ce rôle");
            }
        }

        userService.revokeUserRole(id, role);
    }

    @Override
    public Page<User> searchUsers(String query, Pageable pageable) {
        if (!userService.authenticatedHasRole(UserRole.ADMIN)){
            throw UserPermissionExceptionFactory.forAdminPermissionRequired("rechercher des utilisateurs");
        }
        return userService.searchUsers(query, pageable);
    }

    @Override
    public Page<User> searchUsersByCriteria(String username, String firstname, String lastname, String email, String phoneNumber, Pageable pageable) {
        if (!userService.authenticatedHasRole(UserRole.ADMIN)){
            throw UserPermissionExceptionFactory.forAdminPermissionRequired("rechercher des utilisateurs");
        }
        return userService.searchUsersByCriteria(username, firstname, lastname, email, phoneNumber, pageable);
    }

    @Override
    public User getUserById(Long id) {
        if (!userService.authenticatedHasRole(UserRole.ADMIN)){
            throw UserPermissionExceptionFactory.forAdminPermissionRequired("consulter les détails utilisateur");
        }
        return userService.getUserById(id);
    }

    @Override
    public void triggerPasswordReset(Long id) {
        if (!userService.authenticatedHasRole(UserRole.ADMIN)){
            throw UserPermissionExceptionFactory.forAdminPermissionRequired("déclencher la réinitialisation de mot de passe");
        }

        User user = getUserById(id);
        PasswordResetToken token = passwordResetTokenService.createPasswordResetToken(user);
        mailerService.sendPasswordReset(token.getToken(), user);
    }

    @Override
    public List<Device> getUserDevices(Long id) {
        User user = userService.getUserById(id);
        return deviceService.getUserDevice(user);
    }

    @Override
    public Long getTotalUsers() {
        return userService.getTotalUsers();
    }


    private boolean hasRole(User user, UserRole role) {
        return user.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role.name()));
    }


    private String generateSecurePassword() {
        // Définir les groupes de caractères
        String upperCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialCharacters = "!@#$%^&*()-_=+[]{}|;:,.<>/?";

        // Combiner tous les caractères possibles
        String allCharacters = upperCaseLetters + lowerCaseLetters + numbers + specialCharacters;

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        // Garantir au moins un caractère de chaque type
        password.append(upperCaseLetters.charAt(random.nextInt(upperCaseLetters.length())));
        password.append(lowerCaseLetters.charAt(random.nextInt(lowerCaseLetters.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(specialCharacters.charAt(random.nextInt(specialCharacters.length())));

        // Remplir le reste du mot de passe avec des caractères aléatoires
        for (int i = 4; i < 12; i++) { // Mot de passe de 12 caractères
            password.append(allCharacters.charAt(random.nextInt(allCharacters.length())));
        }

        // Mélanger les caractères pour éviter un pattern prévisible
        return shuffleString(password.toString());
    }

    private String shuffleString(String input) {
        char[] characters = input.toCharArray();
        SecureRandom random = new SecureRandom();

        for (int i = characters.length - 1; i > 0; i--) {
            int randomIndex = random.nextInt(i + 1);
            char temp = characters[i];
            characters[i] = characters[randomIndex];
            characters[randomIndex] = temp;
        }

        return new String(characters);
    }
}