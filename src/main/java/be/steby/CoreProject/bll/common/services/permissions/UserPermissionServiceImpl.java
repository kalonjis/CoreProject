package be.steby.CoreProject.bll.common.services.permissions;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Implémentation du service de permissions utilisateur.
 * ✅ SANS dépendance à UserService pour éviter les cycles.
 */
@Service
@Slf4j
public class UserPermissionServiceImpl implements UserPermissionService {

    @Value("${security.account-deactivation.allow-admin-deactivation:false}")
    private boolean allowAdminSelfDeactivation;

    @Value("${security.account-deactivation.allow-super-admin-deactivation:false}")
    private boolean allowSuperAdminSelfDeactivation;

    @Value("${security.account-deactivation.allow-moderator-deactivation:true}")
    private boolean allowModeratorSelfDeactivation;

    // ===============================
    // MÉTHODES D'AUTORISATION (pures - sans dépendances externes)
    // ===============================

    @Override
    public boolean canDeactivateUser(User actor, User target) {
        if (actor == null || target == null) {
            log.warn("Cannot check deactivation permission: actor or target is null");
            return false;
        }

        // Un utilisateur ne peut pas se désactiver via admin deactivation
        if (actor.getId().equals(target.getId())) {
            log.debug("User {} cannot admin-deactivate themselves", actor.getUsername());
            return false;
        }

        // Vérifier les permissions selon la hiérarchie des rôles
        return canActOnUser(actor, target);
    }

    @Override
    public boolean canSelfDeactivate(User user) {
        if (user == null) {
            log.warn("Cannot check self-deactivation permission: user is null");
            return false;
        }

        Set<UserRole> userRoles = user.getUserRoles();

        // Vérifier les règles spécifiques par rôle (du plus élevé au plus bas)
        if (userRoles.contains(UserRole.SUPER_ADMIN)) {
            log.debug("Super-admin self-deactivation allowed: {}", allowSuperAdminSelfDeactivation);
            return allowSuperAdminSelfDeactivation;
        }

        if (userRoles.contains(UserRole.ADMIN)) {
            log.debug("Admin self-deactivation allowed: {}", allowAdminSelfDeactivation);
            return allowAdminSelfDeactivation;
        }

        if (userRoles.contains(UserRole.MODERATOR)) {
            log.debug("Moderator self-deactivation allowed: {}", allowModeratorSelfDeactivation);
            return allowModeratorSelfDeactivation;
        }

        // USER peut toujours se désactiver
        log.debug("User self-deactivation allowed by default");
        return true;
    }

    @Override
    public boolean canActivateUser(User actor, User target) {
        if (actor == null || target == null) {
            log.warn("Cannot check activation permission: actor or target is null");
            return false;
        }

        // Même logique que la désactivation
        return canActOnUser(actor, target);
    }

    @Override
    public boolean canGrantRole(User actor, User target, UserRole roleToGrant) {
        if (actor == null || target == null || roleToGrant == null) {
            log.warn("Cannot check role grant permission: actor, target, or role is null");
            return false;
        }

        // ✅ LOGIQUE CORRIGÉE : Attribution des rôles
        UserRole actorHighestRole = getHighestRole(actor);

        // SUPER_ADMIN peut attribuer tous les rôles
        if (actorHighestRole == UserRole.SUPER_ADMIN) {
            return true;
        }

        // ADMIN peut attribuer MODERATOR et USER (mais pas ADMIN ou SUPER_ADMIN)
        if (actorHighestRole == UserRole.ADMIN) {
            return roleToGrant == UserRole.MODERATOR || roleToGrant == UserRole.USER;
        }

        // Les autres rôles ne peuvent rien attribuer
        log.debug("User {} with role {} cannot grant role {}",
                actor.getUsername(), actorHighestRole, roleToGrant);
        return false;
    }

    @Override
    public boolean canRevokeRole(User actor, User target, UserRole roleToRevoke) {
        // Même logique que l'attribution
        return canGrantRole(actor, target, roleToRevoke);
    }

    @Override
    public boolean canActOnUser(User actor, User target) {
        if (actor == null || target == null) {
            return false;
        }

        UserRole actorRole = getHighestRole(actor);
        UserRole targetRole = getHighestRole(target);

        // SUPER_ADMIN peut agir sur tout le monde (sauf auto-ciblage selon contexte)
        if (actorRole == UserRole.SUPER_ADMIN) {
            return true;
        }

        // ADMIN peut agir sur MODERATOR et USER (mais pas SUPER_ADMIN)
        if (actorRole == UserRole.ADMIN) {
            return targetRole != UserRole.SUPER_ADMIN;
        }

        // MODERATOR et USER ne peuvent pas agir sur d'autres comptes
        log.debug("User {} with role {} cannot act on user {} with role {}",
                actor.getUsername(), actorRole, target.getUsername(), targetRole);
        return false;
    }

    @Override
    public UserRole getHighestRole(User user) {
        if (user == null || user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            return UserRole.USER; // Rôle par défaut
        }

        Set<UserRole> roles = user.getUserRoles();

        // Ordre hiérarchique décroissant
        if (roles.contains(UserRole.SUPER_ADMIN)) return UserRole.SUPER_ADMIN;
        if (roles.contains(UserRole.ADMIN)) return UserRole.ADMIN;
        if (roles.contains(UserRole.MODERATOR)) return UserRole.MODERATOR;
        return UserRole.USER;
    }

    @Override
    public boolean hasAdminPrivileges(User user) {
        if (user == null) return false;
        UserRole highestRole = getHighestRole(user);
        return highestRole == UserRole.ADMIN || highestRole == UserRole.SUPER_ADMIN;
    }

    @Override
    public boolean hasSuperAdminPrivileges(User user) {
        if (user == null) return false;
        return getHighestRole(user) == UserRole.SUPER_ADMIN;
    }
}
