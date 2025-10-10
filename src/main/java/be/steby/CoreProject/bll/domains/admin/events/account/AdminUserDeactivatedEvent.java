package be.steby.CoreProject.bll.domains.admin.events.account;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;

import java.time.Instant;

/**
 * Événement émis lorsqu'un utilisateur est désactivé par un administrateur.
 * Cet événement contient toutes les informations nécessaires pour l'audit
 * et les notifications liées à la désactivation d'utilisateur par un admin.
 */
public record AdminUserDeactivatedEvent(
        /**
         * L'utilisateur qui vient d'être désactivé.
         */
        User targetUser,

        /**
         * L'administrateur qui a effectué la désactivation.
         */
        User adminUser,

        /**
         * Catégorie administrative de la désactivation.
         */
        AdminDeactivationCategory category,

        /**
         * Commentaire ou détails additionnels.
         */
        String comment,

        /**
         * Indique si la désactivation invalide les sessions actives.
         */
        boolean invalidateActiveSessions,

        /**
         * Date de la dernière activité de l'utilisateur.
         */
        Instant lastActivity,

        /**
         * Timestamp de l'événement.
         */
        Instant timestamp
) {

    /**
     * Constructeur avec timestamp automatique.
     */
    public AdminUserDeactivatedEvent(
            User targetUser,
            User adminUser,
            AdminDeactivationCategory category,
            String comment,
            boolean invalidateActiveSessions,
            Instant lastActivity) {
        this(targetUser, adminUser, category, comment, invalidateActiveSessions, lastActivity, Instant.now());
    }

    /**
     * Crée un événement de désactivation d'utilisateur par admin.
     *
     * @param targetUser L'utilisateur désactivé
     * @param adminUser L'administrateur
     * @param category La catégorie de la désactivation
     * @param comment Commentaire optionnel
     * @param invalidateActiveSessions Si invalider les sessions
     * @param lastActivity Dernière activité de l'utilisateur
     * @return Nouvel événement
     */
    public static AdminUserDeactivatedEvent of(
            User targetUser,
            User adminUser,
            AdminDeactivationCategory category,
            String comment,
            boolean invalidateActiveSessions,
            Instant lastActivity
            ) {
                return new AdminUserDeactivatedEvent(
                    targetUser, adminUser, category, comment, invalidateActiveSessions, lastActivity
                );
    }

    /**
     * Crée un événement de désactivation simple.
     *
     * @param targetUser L'utilisateur désactivé
     * @param adminUser L'administrateur
     * @param category La catégorie de la désactivation
     * @param adminDeactivationDetails Les commentaires de la désactivation
     * @return Nouvel événement
     */
    public static AdminUserDeactivatedEvent simple(
            User targetUser,
            User adminUser,
            AdminDeactivationCategory category,
            String adminDeactivationDetails) {
                return new AdminUserDeactivatedEvent(
                        targetUser, adminUser, category, adminDeactivationDetails, true, null
                );
    }

    /**
     * Vérifie si la désactivation est pour des raisons de sécurité.
     *
     * @return true si la catégorie est liée à la sécurité
     */
    public boolean isSecurityRelated() {
        return category.name().startsWith("SECURITY_RISK_") ||
                category.name().startsWith("BANNED_") ||
                category == AdminDeactivationCategory.TOS_VIOLATION_FRAUD;
    }

    /**
     * Vérifie si la désactivation est temporaire.
     *
     * @return true si la catégorie permet la réactivation
     */
    public boolean isTemporary() {
        return category.allowsReactivation();
    }

    /**
     * Calcule la durée d'inactivité avant désactivation.
     *
     * @return Durée en millisecondes ou 0 si non applicable
     */
    public long getInactivityDurationMs() {
        if (lastActivity == null) {
            return 0;
        }
        return timestamp.toEpochMilli() - lastActivity.toEpochMilli();
    }

    /**
     * Obtient l'ID de l'utilisateur désactivé.
     *
     * @return ID de l'utilisateur désactivé
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
     * Obtient le nom d'utilisateur de l'utilisateur désactivé.
     *
     * @return Nom d'utilisateur désactivé
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
     * Vérifie si un commentaire a été fourni.
     *
     * @return true si un commentaire est présent
     */
    public boolean hasComment() {
        return comment != null && !comment.isBlank();
    }

    /**
     * Vérifie si la désactivation concerne un utilisateur avec des rôles administratifs.
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
                "Utilisateur '%s' désactivé par l'administrateur '%s' (Catégorie: %s)",
                getTargetUsername(),
                getAdminUsername(),
                category.getDisplayName()
        );

        if (hasComment()) {
            base += ". Commentaire: " + comment;
        }

        if (invalidateActiveSessions) {
            base += " - Sessions actives invalidées";
        }

        return base;
    }

    /**
     * Obtient le niveau de sévérité de la désactivation.
     *
     * @return Niveau de sévérité (LOW, MEDIUM, HIGH)
     */
    public String getSeverityLevel() {
        int severity = category.getSeverityLevel();
        return switch (severity) {
            case 1, 2 -> "LOW";
            case 3 -> "MEDIUM";
            case 4, 5 -> "HIGH";
            default -> "MEDIUM";
        };
    }
}