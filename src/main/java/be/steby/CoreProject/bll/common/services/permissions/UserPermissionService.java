package be.steby.CoreProject.bll.common.services.permissions;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;

/**
 * Service pour gérer les permissions d'action entre utilisateurs.
 * ✅ NE DÉPEND PAS de UserService pour éviter les cycles.
 * Les méthodes prennent des User en paramètre au lieu d'aller les chercher.
 */
public interface UserPermissionService {

    /**
     * Vérifie si un utilisateur peut désactiver un autre utilisateur.
     */
    boolean canDeactivateUser(User actor, User target);

    /**
     * Vérifie si un utilisateur peut se désactiver lui-même.
     */
    boolean canSelfDeactivate(User user);

    /**
     * Vérifie si un utilisateur peut activer un autre utilisateur.
     */
    boolean canActivateUser(User actor, User target);

    /**
     * Vérifie si un utilisateur peut accorder un rôle à un autre utilisateur.
     */
    boolean canGrantRole(User actor, User target, UserRole roleToGrant);

    /**
     * Vérifie si un utilisateur peut révoquer un rôle d'un autre utilisateur.
     */
    boolean canRevokeRole(User actor, User target, UserRole roleToRevoke);

    /**
     * Vérifie si un utilisateur peut effectuer une action sur un autre utilisateur
     * en se basant sur la hiérarchie des rôles.
     */
    boolean canActOnUser(User actor, User target);

    /**
     * Obtient le rôle le plus élevé d'un utilisateur.
     */
    UserRole getHighestRole(User user);

    /**
     * Vérifie si un utilisateur a des privilèges administratifs.
     */
    boolean hasAdminPrivileges(User user);

    /**
     * Vérifie si un utilisateur a des privilèges de super admin.
     */
    boolean hasSuperAdminPrivileges(User user);

}