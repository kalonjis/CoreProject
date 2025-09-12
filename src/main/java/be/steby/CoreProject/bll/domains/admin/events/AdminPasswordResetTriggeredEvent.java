package be.steby.CoreProject.bll.domains.admin.events;

import be.steby.CoreProject.dl.entities.User;

import java.time.Instant;

/**
 * Événement émis lorsqu'un administrateur déclenche un reset de mot de passe pour un utilisateur.
 * Cet événement contient toutes les informations nécessaires pour l'audit
 * et les notifications liées aux resets de mot de passe déclenchés par un admin.
 */
public record AdminPasswordResetTriggeredEvent(
        /**
         * L'utilisateur pour qui le reset de mot de passe est déclenché.
         */
        User targetUser,

        /**
         * L'administrateur qui a déclenché le reset.
         */
        User adminUser,

        /**
         * Raison du reset de mot de passe (optionnel).
         */
        String reason,

        /**
         * Indique si l'utilisateur sera forcé de changer son mot de passe à la prochaine connexion.
         */
        boolean forceChangeOnNextLogin,

        /**
         * Indique si les sessions actives doivent être invalidées.
         */
        boolean invalidateActiveSessions,

        /**
         * Email de notification envoyé à l'utilisateur.
         */
        String notificationEmail,

        /**
         * Timestamp de l'événement.
         */
        Instant timestamp
) {

    /**
     * Constructeur avec timestamp automatique.
     */
    public AdminPasswordResetTriggeredEvent(
            User targetUser,
            User adminUser,
            String reason,
            boolean forceChangeOnNextLogin,
            boolean invalidateActiveSessions,
            String notificationEmail) {
        this(targetUser, adminUser, reason, forceChangeOnNextLogin, invalidateActiveSessions,
                notificationEmail, Instant.now());
    }

    /**
     * Crée un événement de reset de mot de passe déclenché par admin.
     *
     * @param targetUser L'utilisateur cible
     * @param adminUser L'administrateur
     * @param reason La raison du reset
     * @param forceChangeOnNextLogin Si forcer le changement
     * @param invalidateActiveSessions Si invalider les sessions
     * @param notificationEmail Email de notification
     * @return Nouvel événement
     */
    public static AdminPasswordResetTriggeredEvent of(
            User targetUser,
            User adminUser,
            String reason,
            boolean forceChangeOnNextLogin,
            boolean invalidateActiveSessions,
            String notificationEmail) {
        return new AdminPasswordResetTriggeredEvent(
                targetUser, adminUser, reason, forceChangeOnNextLogin,
                invalidateActiveSessions, notificationEmail
        );
    }

    /**
     * Crée un événement de reset simple.
     *
     * @param targetUser L'utilisateur cible
     * @param adminUser L'administrateur
     * @return Nouvel événement
     */
    public static AdminPasswordResetTriggeredEvent simple(
            User targetUser,
            User adminUser) {
        return new AdminPasswordResetTriggeredEvent(
                targetUser, adminUser, null, true, true,
                targetUser.getEmail()
        );
    }

    /**
     * Vérifie si le reset est pour des raisons de sécurité.
     *
     * @return true si la raison suggère un problème de sécurité
     */
    public boolean isSecurityRelated() {
        if (reason == null) return false;
        String lowerReason = reason.toLowerCase();
        return lowerReason.contains("sécurité") ||
                lowerReason.contains("security") ||
                lowerReason.contains("compromis") ||
                lowerReason.contains("breach") ||
                lowerReason.contains("suspicious");
    }

    /**
     * Vérifie si c'est un reset urgent.
     *
     * @return true si c'est un reset urgent (sécurité + invalidation sessions)
     */
    public boolean isUrgentReset() {
        return isSecurityRelated() && invalidateActiveSessions;
    }

    /**
     * Obtient l'ID de l'utilisateur cible.
     *
     * @return ID de l'utilisateur cible
     */
    public Long getTargetUserId() {
        return targetUser.getId();
    }

    /**
     * Obtient l'ID de l'administrateur.
     *
     * @return ID de l'administrateur
     */
    public Long getAdminUserId() {
        return adminUser.getId();
    }

    /**
     * Obtient le nom d'utilisateur de l'utilisateur cible.
     *
     * @return Nom d'utilisateur cible
     */
    public String getTargetUsername() {
        return targetUser.getUsername();
    }

    /**
     * Obtient le nom d'utilisateur de l'administrateur.
     *
     * @return Nom d'utilisateur de l'admin
     */
    public String getAdminUsername() {
        return adminUser.getUsername();
    }

    /**
     * Vérifie si une raison a été fournie.
     *
     * @return true si une raison est présente
     */
    public boolean hasReason() {
        return reason != null && !reason.isBlank();
    }

    /**
     * Vérifie si un email de notification a été spécifié.
     *
     * @return true si un email est spécifié
     */
    public boolean hasNotificationEmail() {
        return notificationEmail != null && !notificationEmail.isBlank();
    }

    /**
     * Vérifie si le reset concerne un utilisateur avec des rôles administratifs.
     *
     * @return true si l'utilisateur a des rôles d'administration
     */
    public boolean isAdministrativeUser() {
        return targetUser.getUserRoles().stream().anyMatch(role ->
                role.name().contains("ADMIN") || role.name().contains("MODERATOR")
        );
    }

    /**
     * Crée une description textuelle de l'événement.
     *
     * @return Description de l'événement
     */
    public String getDescription() {
        String base = String.format(
                "Reset de mot de passe déclenché pour l'utilisateur '%s' par l'administrateur '%s'",
                getTargetUsername(),
                getAdminUsername()
        );

        if (hasReason()) {
            base += ". Raison: " + reason;
        }

        if (forceChangeOnNextLogin) {
            base += " - Changement obligatoire à la prochaine connexion";
        }

        if (invalidateActiveSessions) {
            base += " - Sessions actives invalidées";
        }

        return base;
    }

    /**
     * Obtient le niveau de priorité du reset.
     *
     * @return Niveau de priorité (LOW, MEDIUM, HIGH, URGENT)
     */
    public String getPriorityLevel() {
        if (isUrgentReset()) {
            return "URGENT";
        } else if (isSecurityRelated()) {
            return "HIGH";
        } else if (forceChangeOnNextLogin) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * Crée un résumé des actions effectuées.
     *
     * @return Résumé des actions
     */
    public String getActionSummary() {
        StringBuilder summary = new StringBuilder("Reset de mot de passe");

        if (forceChangeOnNextLogin) {
            summary.append(" + Changement forcé");
        }

        if (invalidateActiveSessions) {
            summary.append(" + Invalidation sessions");
        }

        if (hasNotificationEmail()) {
            summary.append(" + Notification email");
        }

        return summary.toString();
    }
}