package be.steby.CoreProject.bll.common.exceptions;

import be.steby.CoreProject.dl.enums.UserRole;

/**
 * Factory pour créer des exceptions de permissions utilisateur standardisées.
 * Centralise la création d'exceptions avec des messages cohérents et appropriés.
 *
 * VERSION CONSOLIDÉE - Remplace SelfManagementException
 */
public class UserPermissionExceptionFactory {

    /**
     * Exception pour auto-désactivation avec message adapté au rôle spécifique.
     * ✅ Version propre utilisant UserRole en paramètre
     */
    public static UserPermissionException forSelfDeactivationByRole(UserRole userRole) {
        return switch (userRole) {
            case SUPER_ADMIN -> new UserPermissionException(
                    "PROCÉDURE ADMINISTRATIVE REQUISE : Les super-administrateurs ne peuvent pas se désactiver. " +
                            "Contactez un autre super-administrateur pour gérer la désactivation de votre compte et " +
                            "assurer la transition des responsabilités."
            );
            case ADMIN -> new UserPermissionException(
                    "PROCÉDURE ADMINISTRATIVE REQUISE : Les administrateurs ne peuvent pas se désactiver. " +
                            "Contactez un autre administrateur pour gérer la désactivation de votre compte et " +
                            "assurer la transition des responsabilités."
            );
            case MODERATOR -> new UserPermissionException(
                    "PROCÉDURE ADMINISTRATIVE REQUISE : Les modérateurs ne peuvent pas se désactiver. " +
                            "Contactez un administrateur pour gérer la désactivation de votre compte et " +
                            "assurer la transition des responsabilités de modération."
            );
            default -> new UserPermissionException(
                    "PROCÉDURE ADMINISTRATIVE REQUISE : Vous ne pouvez pas désactiver votre propre compte. " +
                            "Contactez un administrateur pour gérer la désactivation de votre compte."
            );
        };
    }

    /**
     * Exception pour auto-activation avec message adapté au rôle spécifique.
     */
    public static UserPermissionException forSelfActivationByRole(UserRole userRole) {
        return switch (userRole) {
            case SUPER_ADMIN -> new UserPermissionException(
                    "PROCÉDURE ADMINISTRATIVE REQUISE : Les super-administrateurs ne peuvent pas s'activer. " +
                            "Contactez un autre super-administrateur pour gérer l'activation de votre compte."
            );
            case ADMIN -> new UserPermissionException(
                    "PROCÉDURE ADMINISTRATIVE REQUISE : Les administrateurs ne peuvent pas s'activer. " +
                            "Contactez un autre administrateur pour gérer l'activation de votre compte."
            );
            case MODERATOR -> new UserPermissionException(
                    "PROCÉDURE ADMINISTRATIVE REQUISE : Les modérateurs ne peuvent pas s'activer. " +
                            "Contactez un administrateur pour gérer l'activation de votre compte."
            );
            default -> new UserPermissionException(
                    "PROCÉDURE ADMINISTRATIVE REQUISE : Vous ne pouvez pas activer votre propre compte. " +
                            "Contactez un administrateur pour activer votre compte."
            );
        };
    }

    /**
     * Exception pour une tentative d'auto-suppression non autorisée.
     */
    public static UserPermissionException forSelfDeletion() {
        return new UserPermissionException(
                "PROCÉDURE ADMINISTRATIVE REQUISE : Vous ne pouvez pas supprimer votre propre compte. " +
                        "Contactez un autre administrateur pour gérer la suppression de votre compte."
        );
    }

    /**
     * Exception pour une tentative de modification de ses propres rôles.
     */
    public static UserPermissionException forSelfRoleManagement(String operation) {
        return new UserPermissionException(
                String.format("PROCÉDURE ADMINISTRATIVE REQUISE : Vous ne pouvez pas %s vos propres rôles. " +
                        "Contactez un autre administrateur pour modifier vos rôles.", operation)
        );
    }

    // endregion

    // region Policy-Based Self-Deactivation Exceptions

    /**
     * Exception pour un administrateur qui ne peut pas se désactiver selon les règles configurées.
     */
    public static UserPermissionException forAdminSelfDeactivationPolicy() {
        return new UserPermissionException(
                "PROCÉDURE ADMINISTRATIVE REQUISE : Les administrateurs ne peuvent pas se désactiver selon la politique de sécurité. " +
                        "Contactez un autre administrateur pour gérer la désactivation de votre compte et " +
                        "assurer la transition des responsabilités."
        );
    }

    /**
     * Exception pour un super-administrateur qui ne peut pas se désactiver selon les règles configurées.
     */
    public static UserPermissionException forSuperAdminSelfDeactivationPolicy() {
        return new UserPermissionException(
                "PROCÉDURE ADMINISTRATIVE REQUISE : Les super-administrateurs ne peuvent pas se désactiver selon la politique de sécurité. " +
                        "Contactez un autre super-administrateur pour gérer la désactivation de votre compte et " +
                        "assurer la transition des responsabilités."
        );
    }

    /**
     * Exception pour un modérateur qui ne peut pas se désactiver selon les règles configurées.
     */
    public static UserPermissionException forModeratorSelfDeactivationPolicy() {
        return new UserPermissionException(
                "PROCÉDURE ADMINISTRATIVE REQUISE : Les modérateurs ne peuvent pas se désactiver selon la politique de sécurité. " +
                        "Contactez un administrateur pour gérer la désactivation de votre compte et " +
                        "assurer la transition des responsabilités de modération."
        );
    }

    // endregion

    // region Administrative Permission Exceptions

    /**
     * Exception pour une tentative d'action sur un SUPER_ADMIN sans permissions suffisantes.
     */
    public static UserPermissionException forSuperAdminAction(String action) {
        return new UserPermissionException(
                String.format("Impossible de %s un utilisateur SUPER_ADMIN. " +
                        "Seul un SUPER_ADMIN peut effectuer cette action.", action)
        );
    }

    /**
     * Exception pour une tentative d'octroi du rôle SUPER_ADMIN sans permissions suffisantes.
     */
    public static UserPermissionException forSuperAdminRoleGrant() {
        return new UserPermissionException(
                "Impossible d'accorder le rôle SUPER_ADMIN. " +
                        "Seul un SUPER_ADMIN peut accorder ce rôle."
        );
    }

    /**
     * Exception pour une tentative de révocation du rôle SUPER_ADMIN sans permissions suffisantes.
     */
    public static UserPermissionException forSuperAdminRoleRevoke() {
        return new UserPermissionException(
                "Impossible de révoquer le rôle SUPER_ADMIN. " +
                        "Seul un SUPER_ADMIN peut révoquer ce rôle."
        );
    }

    /**
     * Exception pour une tentative d'action administrative sans permissions d'admin.
     */
    public static UserPermissionException forAdminPermissionRequired(String action) {
        return new UserPermissionException(
                String.format("Permissions administrateur requises pour %s. " +
                        "Contactez un administrateur.", action)
        );
    }

    // endregion

    // region Generic Permission Exceptions

    /**
     * Exception générique pour permissions insuffisantes.
     */
    public static UserPermissionException forInsufficientPermissions(UserRole requiredRole, String action) {
        return new UserPermissionException(
                String.format("Permissions insuffisantes pour %s. Rôle requis : %s.", action, requiredRole.name())
        );
    }

    /**
     * Exception pour une tentative d'action sur un utilisateur sans autorisation.
     */
    public static UserPermissionException forUnauthorizedUserAction(String action, UserRole targetRole) {
        return new UserPermissionException(
                String.format("Action non autorisée : impossible de %s un utilisateur avec le rôle %s.",
                        action, targetRole.name())
        );
    }

    /**
     * Exception pour des paramètres invalides.
     */
    public static UserPermissionException forInvalidParameters(String reason) {
        return new UserPermissionException(
                String.format("Paramètres invalides : %s", reason), 400
        );
    }

    // endregion

    // region Specific Deactivation Context Exceptions

    /**
     * Exception spécifique pour admin-deactivation d'un même compte.
     * Distingue le contexte admin du contexte self-deactivation.
     */
    public static UserPermissionException forAdminSelfTargeting() {
        return new UserPermissionException(
                "Un administrateur ne peut pas utiliser la désactivation administrative sur son propre compte. " +
                        "Utilisez la procédure de self-deactivation ou contactez un autre administrateur."
        );
    }

    /**
     * Exception pour tentative de bypass des règles de self-deactivation.
     */
    public static UserPermissionException forSelfDeactivationPolicyBypass() {
        return new UserPermissionException(
                "Contournement de politique détecté : Vous ne pouvez pas utiliser les fonctions administratives " +
                        "pour contourner les restrictions de self-deactivation. Contactez un autre administrateur."
        );
    }

    // endregion
}