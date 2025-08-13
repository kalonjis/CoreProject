package be.steby.CoreProject.bll.common.services.permissions;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Implémentation du service de permissions utilisateur.
 * Centralise la logique d'autorisation pour les opérations sur les comptes utilisateurs.
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

        // Les utilisateurs normaux (USER, GUEST) peuvent toujours se désactiver
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canActivateUser(User actor, User target) {
        if (actor == null || target == null) {
            log.warn("Cannot check activation permission: actor or target is null");
            return false;
        }

        // Un utilisateur ne peut pas s'activer lui-même
        if (actor.getId().equals(target.getId())) {
            log.debug("User {} cannot self-activate", actor.getUsername());
            return false;
        }

        // Vérifier les permissions selon la hiérarchie des rôles
        return canActOnUser(actor, target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canGrantRole(User actor, User target, UserRole roleToGrant) {
        if (actor == null || target == null || roleToGrant == null) {
            log.warn("Cannot check role grant permission: null parameter");
            return false;
        }

        // Un utilisateur ne peut pas modifier ses propres rôles
        if (actor.getId().equals(target.getId())) {
            log.debug("User {} cannot grant role to themselves", actor.getUsername());
            return false;
        }

        // Seul un SUPER_ADMIN peut accorder le rôle SUPER_ADMIN
        if (roleToGrant == UserRole.SUPER_ADMIN && !actor.getUserRoles().contains(UserRole.SUPER_ADMIN)) {
            log.debug("User {} cannot grant SUPER_ADMIN role without being SUPER_ADMIN", actor.getUsername());
            return false;
        }

        // L'acteur doit avoir au moins les permissions d'admin pour accorder des rôles
        return hasMinimumRole(actor, UserRole.ADMIN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canRevokeRole(User actor, User target, UserRole roleToRevoke) {
        if (actor == null || target == null || roleToRevoke == null) {
            log.warn("Cannot check role revoke permission: null parameter");
            return false;
        }

        // Un utilisateur ne peut pas modifier ses propres rôles
        if (actor.getId().equals(target.getId())) {
            log.debug("User {} cannot revoke their own roles", actor.getUsername());
            return false;
        }

        // Seul un SUPER_ADMIN peut révoquer le rôle SUPER_ADMIN
        if (roleToRevoke == UserRole.SUPER_ADMIN && !actor.getUserRoles().contains(UserRole.SUPER_ADMIN)) {
            log.debug("User {} cannot revoke SUPER_ADMIN role without being SUPER_ADMIN", actor.getUsername());
            return false;
        }

        // L'acteur doit avoir au moins les permissions d'admin pour révoquer des rôles
        return hasMinimumRole(actor, UserRole.ADMIN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canActOnUser(User actor, User target) {
        if (actor == null || target == null) {
            return false;
        }

        UserRole actorHighestRole = getHighestRole(actor);
        UserRole targetHighestRole = getHighestRole(target);

        // Utilisation de la logique intégrée dans UserRole
        return actorHighestRole.canActOn(targetHighestRole);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserRole getHighestRole(User user) {
        if (user == null || user.getUserRoles() == null) {
            return UserRole.GUEST;
        }

        // Délégation à la méthode statique de UserRole
        return UserRole.getHighestRole(user.getUserRoles());
    }

    /**
     * Vérifie si un utilisateur a au minimum le rôle spécifié.
     *
     * @param user L'utilisateur à vérifier
     * @param minimumRole Le rôle minimum requis
     * @return true si l'utilisateur a au moins ce rôle
     */
    private boolean hasMinimumRole(User user, UserRole minimumRole) {
        if (user == null || user.getUserRoles() == null) {
            return false;
        }

        // Délégation à la méthode statique de UserRole
        return UserRole.hasMinimumRole(user.getUserRoles(), minimumRole);
    }
}