package be.steby.CoreProject.bll.domains.admin.events.account;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

import java.time.Instant;

/**
 * Événement émis lorsqu'un utilisateur est activé par un administrateur.
 * Cet événement contient toutes les informations nécessaires pour l'audit
 * et les notifications liées à l'activation d'utilisateur par un admin.
 */
public record AdminUserActivatedEvent(
        /**
         * L'utilisateur qui vient d'être activé.
         */
        User targetUser,

        /**
         * L'administrateur qui a effectué l'activation.
         */
        User adminUser,

        /**
         * The device used to perform the creation.
         */
        Device device,

        /**
         * Indique si l'utilisateur était précédemment désactivé.
         */
        boolean wasPreviouslyDeactivated,

        /**
         * Date de la dernière désactivation (si applicable).
         */
        Instant lastDeactivationDate,

        /**
         * Timestamp de l'événement.
         */
        Instant timestamp
) {

    /**
     * Constructeur avec timestamp automatique.
     */
    public AdminUserActivatedEvent(
            User targetUser,
            User adminUser,
            Device device,
            boolean wasPreviouslyDeactivated,
            Instant lastDeactivationDate) {
        this(targetUser, adminUser, device, wasPreviouslyDeactivated, lastDeactivationDate, Instant.now());
    }

    /**
     * Crée un événement d'activation d'utilisateur par admin.
     *
     * @param targetUser L'utilisateur activé
     * @param adminUser L'administrateur
     * @param device The device used to activate the user
     * @param wasPreviouslyDeactivated Si l'utilisateur était désactivé
     * @param lastDeactivationDate Date de dernière désactivation
     * @return Nouvel événement
     */
    public static AdminUserActivatedEvent of(
            User targetUser,
            User adminUser,
            Device device,
            boolean wasPreviouslyDeactivated,
            Instant lastDeactivationDate) {

                return new AdminUserActivatedEvent(
                targetUser, adminUser, device, wasPreviouslyDeactivated, lastDeactivationDate
                );
    }

    /**
     * Crée un événement d'activation simple.
     *
     * @param targetUser L'utilisateur activé
     * @param adminUser L'administrateur
     * @param device The device used to activate the user
     * @return Nouvel événement
     */
    public static AdminUserActivatedEvent simple(
            User targetUser,
            User adminUser,
            Device device
    ) {
        return new AdminUserActivatedEvent(
                targetUser,
                adminUser,
                device,
                false, null
        );
    }

    /**
     * Vérifie si c'est une réactivation après désactivation.
     *
     * @return true si l'utilisateur était précédemment désactivé
     */
    public boolean isReactivation() {
        return wasPreviouslyDeactivated;
    }

    /**
     * Calcule la durée de désactivation si applicable.
     *
     * @return Durée en millisecondes ou 0 si non applicable
     */
    public long getDeactivationDurationMs() {
        if (!wasPreviouslyDeactivated || lastDeactivationDate == null) {
            return 0;
        }
        return timestamp.toEpochMilli() - lastDeactivationDate.toEpochMilli();
    }

    /**
     * Obtient l'ID de l'utilisateur activé.
     *
     * @return ID de l'utilisateur activé
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
     * Obtient le nom d'utilisateur de l'utilisateur activé.
     *
     * @return Nom d'utilisateur activé
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
     * Vérifie si l'activation concerne un utilisateur avec des rôles administratifs.
     *
     * @return true si l'utilisateur a des rôles d'administration
     */
    public boolean isAdministrativeUser() {
        return targetUser.getUserRoles().stream().anyMatch(role ->
                role.name().contains("ADMIN")
        );
    }

    /**
     * Crée une description textuelle de l'événement.
     *
     * @return Description de l'événement
     */
    public String getDescription() {
        String action = isReactivation() ? "réactivé" : "activé";
        String base = String.format(
                "Utilisateur '%s' %s par l'administrateur '%s'",
                getTargetUsername(),
                action,
                getAdminUsername()
        );

        if (isReactivation() && getDeactivationDurationMs() > 0) {
            long days = getDeactivationDurationMs() / (1000 * 60 * 60 * 24);
            base += String.format(" (désactivé pendant %d jour(s))", days);
        }

        return base;
    }

    /**
     * Obtient le type d'activation.
     *
     * @return Type d'activation (ACTIVATION ou REACTIVATION)
     */
    public String getActivationType() {
        return isReactivation() ? "REACTIVATION" : "ACTIVATION";
    }
}