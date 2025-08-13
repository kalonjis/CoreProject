package be.steby.CoreProject.bll.common.exceptions;

import be.steby.CoreProject.dl.enums.UserRole;

/**
 * Factory pour créer des exceptions de permissions utilisateur standardisées.
 * Centralise la création d'exceptions avec des messages cohérents et appropriés.
 */
public class UserPermissionExceptionFactory {

    // region Self-Management Exceptions

    /**
     * Exception pour une tentative d'auto-désactivation non autorisée.
     *
     * @return UserPermissionException configurée pour l'auto-désactivation
     */
    public static UserPermissionException forSelfDeactivation() {
        return new UserPermissionException(
                "Vous ne pouvez pas désactiver votre propre compte. " +
                        "Contactez un autre administrateur pour gérer la désactivation de votre compte."
        );
    }

    /**
     * Exception pour une tentative d'auto-activation non autorisée.
     *
     * @return UserPermissionException configurée pour l'auto-activation
     */
    public static UserPermissionException forSelfActivation() {
        return new UserPermissionException(
                "Vous ne pouvez pas activer votre propre compte. " +
                        "Contactez un administrateur pour activer votre compte."
        );
    }

    /**
     * Exception pour une tentative de modification de ses propres rôles.
     *
     * @param operation L'opération tentée (grant/revoke)
     * @return UserPermissionException configurée pour la gestion des rôles
     */
    public static UserPermissionException forSelfRoleManagement(String operation) {
        return new UserPermissionException(
                String.format("Vous ne pouvez pas %s vos propres rôles. " +
                        "Contactez un autre administrateur pour modifier vos rôles.", operation)
        );
    }

    /**
     * Exception pour un modérateur qui ne peut pas se désactiver selon les règles configurées.
     *
     * @return UserPermissionException configurée pour la politique modérateur
     */
    public static UserPermissionException forModeratorSelfDeactivationPolicy() {
        return new UserPermissionException(
                "PROCÉDURE ADMINISTRATIVE REQUISE : Les modérateurs ne peuvent pas se désactiver. " +
                        "Contactez un administrateur pour gérer la désactivation de votre compte et " +
                        "assurer la transition des responsabilités de modération."
        );
    }

    // endregion

    // region Administrative Permission Exceptions

    /**
     * Exception pour une tentative d'action sur un SUPER_ADMIN sans permissions suffisantes.
     *
     * @param action L'action tentée (désactiver, activer, etc.)
     * @return UserPermissionException configurée pour les actions sur SUPER_ADMIN
     */
    public static UserPermissionException forSuperAdminAction(String action) {
        return new UserPermissionException(
                String.format("Impossible de %s un utilisateur SUPER_ADMIN. " +
                        "Seul un SUPER_ADMIN peut effectuer cette action.", action)
        );
    }

    /**
     * Exception pour une tentative d'octroi du rôle SUPER_ADMIN sans permissions suffisantes.
     *
     * @return UserPermissionException configurée pour l'octroi du rôle SUPER_ADMIN
     */
    public static UserPermissionException forSuperAdminRoleGrant() {
        return new UserPermissionException(
                "Impossible d'accorder le rôle SUPER_ADMIN. " +
                        "Seul un SUPER_ADMIN peut accorder ce rôle."
        );
    }

    /**
     * Exception pour une tentative de révocation du rôle SUPER_ADMIN sans permissions suffisantes.
     *
     * @return UserPermissionException configurée pour la révocation du rôle SUPER_ADMIN
     */
    public static UserPermissionException forSuperAdminRoleRevoke() {
        return new UserPermissionException(
                "Impossible de révoquer le rôle SUPER_ADMIN. " +
                        "Seul un SUPER_ADMIN peut révoquer ce rôle."
        );
    }

    /**
     * Exception pour une tentative d'action administrative sans permissions d'admin.
     *
     * @param action L'action tentée
     * @return UserPermissionException configurée pour les permissions admin
     */
    public static UserPermissionException forAdminPermissionRequired(String action) {
        return new UserPermissionException(
                String.format("Permissions administrateur requises pour %s. " +
                        "Contactez un administrateur.", action)
        );
    }

    // endregion

    // region Self-Deactivation Policy Exceptions

    /**
     * Exception pour un administrateur qui ne peut pas se désactiver selon les règles configurées.
     *
     * @return UserPermissionException configurée pour la politique admin
     */
    public static UserPermissionException forAdminSelfDeactivationPolicy() {
        return new UserPermissionException(
                "PROCÉDURE ADMINISTRATIVE REQUISE : Les administrateurs ne peuvent pas se désactiver. " +
                        "Contactez un autre administrateur pour gérer la désactivation de votre compte et " +
                        "assurer la transition des responsabilités."
        );
    }

    /**
     * Exception pour un super-administrateur qui ne peut pas se désactiver selon les règles configurées.
     *
     * @return UserPermissionException configurée pour la politique super-admin
     */
    public static UserPermissionException forSuperAdminSelfDeactivationPolicy() {
        return new UserPermissionException(
                "PROCÉDURE ADMINISTRATIVE REQUISE : Les super-administrateurs ne peuvent pas se désactiver. " +
                        "Contactez un autre super-administrateur pour gérer la désactivation de votre compte et " +
                        "assurer la transition des responsabilités."
        );
    }

    // endregion

    // region Generic Permission Exceptions

    /**
     * Exception générique pour permissions insuffisantes.
     *
     * @param requiredRole Le rôle minimum requis
     * @param action L'action tentée
     * @return UserPermissionException configurée pour les permissions insuffisantes
     */
    public static UserPermissionException forInsufficientPermissions(UserRole requiredRole, String action) {
        return new UserPermissionException(
                String.format("Permissions insuffisantes pour %s. Rôle requis : %s.", action, requiredRole.name())
        );
    }

    /**
     * Exception pour une tentative d'action sur un utilisateur sans autorisation.
     *
     * @param action L'action tentée
     * @param targetRole Le rôle de l'utilisateur cible
     * @return UserPermissionException configurée pour l'action non autorisée
     */
    public static UserPermissionException forUnauthorizedUserAction(String action, UserRole targetRole) {
        return new UserPermissionException(
                String.format("Action non autorisée : impossible de %s un utilisateur avec le rôle %s.",
                        action, targetRole.name())
        );
    }

    /**
     * Exception pour des paramètres invalides.
     *
     * @param reason La raison de l'invalidité
     * @return UserPermissionException configurée pour les paramètres invalides
     */
    public static UserPermissionException forInvalidParameters(String reason) {
        return new UserPermissionException(
                String.format("Paramètres invalides : %s", reason), 400
        );
    }

    // endregion
}