package be.steby.CoreProject.bll.common.services.permissions;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;

/**
 * Service pour gérer les permissions d'action entre utilisateurs.
 * Centralise la logique d'autorisation pour les opérations sur les comptes utilisateurs.
 */
public interface UserPermissionService {

    /**
     * Vérifie si un utilisateur peut désactiver un autre utilisateur.
     *
     * @param actor L'utilisateur qui effectue l'action
     * @param target L'utilisateur cible de l'action
     * @return true si l'action est autorisée, false sinon
     */
    boolean canDeactivateUser(User actor, User target);

    /**
     * Vérifie si un utilisateur peut se désactiver lui-même.
     *
     * @param user L'utilisateur qui souhaite se désactiver
     * @return true si l'auto-désactivation est autorisée, false sinon
     */
    boolean canSelfDeactivate(User user);

    /**
     * Vérifie si un utilisateur peut activer un autre utilisateur.
     *
     * @param actor L'utilisateur qui effectue l'action
     * @param target L'utilisateur cible de l'action
     * @return true si l'action est autorisée, false sinon
     */
    boolean canActivateUser(User actor, User target);

    /**
     * Vérifie si un utilisateur peut accorder un rôle à un autre utilisateur.
     *
     * @param actor L'utilisateur qui effectue l'action
     * @param target L'utilisateur cible de l'action
     * @param roleToGrant Le rôle à accorder
     * @return true si l'action est autorisée, false sinon
     */
    boolean canGrantRole(User actor, User target, UserRole roleToGrant);

    /**
     * Vérifie si un utilisateur peut révoquer un rôle d'un autre utilisateur.
     *
     * @param actor L'utilisateur qui effectue l'action
     * @param target L'utilisateur cible de l'action
     * @param roleToRevoke Le rôle à révoquer
     * @return true si l'action est autorisée, false sinon
     */
    boolean canRevokeRole(User actor, User target, UserRole roleToRevoke);

    /**
     * Vérifie si un utilisateur peut effectuer une action sur un autre utilisateur
     * en se basant sur la hiérarchie des rôles.
     *
     * @param actor L'utilisateur qui effectue l'action
     * @param target L'utilisateur cible de l'action
     * @return true si l'action est autorisée selon la hiérarchie, false sinon
     */
    boolean canActOnUser(User actor, User target);

    /**
     * Obtient le rôle le plus élevé d'un utilisateur.
     *
     * @param user L'utilisateur
     * @return Le rôle le plus élevé
     */
    UserRole getHighestRole(User user);
}