//package be.steby.CoreProject.dl.enums;
//
///**
// * Types d'actions utilisateur à enregistrer dans les logs de connexion.
// * Format standardisé : CATEGORIE_ACTION
// */
//public enum oldActionLogType {
//    // AUTHENTIFICATION
//    AUTH_LOGIN("Connexion utilisateur"),
//    AUTH_LOGOUT("Déconnexion utilisateur"),
//    AUTH_LOGIN_FAILED("Tentative de connexion échouée"),
//    AUTH_TOKEN_REFRESH("Rafraîchissement du token d'authentification"),
//    AUTH_2FA_SETUP("Configuration de l'authentification à deux facteurs"),
//    AUTH_2FA_VERIFICATION("Vérification de l'authentification à deux facteurs"),
//
//    // COMPTE UTILISATEUR
//    ACCOUNT_CREATED("Création de compte utilisateur"),
//    ACCOUNT_ACTIVATED("Activation de compte utilisateur"),
//    ACCOUNT_DEACTIVATED("Désactivation de compte utilisateur"),
//    ACCOUNT_DELETED("Suppression de compte utilisateur"),
//    ACCOUNT_LOCKED("Verrouillage de compte utilisateur"),
//    ACCOUNT_UNLOCKED("Déverrouillage de compte utilisateur"),
//    ACCOUNT_REACTIVATION_COMPLETED("Réactivation de compte confirmée"),
//
//    // DEMANDES DE COMPTE (NOUVELLES ADDITIONS)
//    ACCOUNT_ACTIVATION_REQUESTED("Demande d'activation de compte utilisateur"),
//    ACCOUNT_DEACTIVATION_REQUESTED("Demande de désactivation de compte utilisateur"),
//    ACCOUNT_REACTIVATION_REQUESTED("Demande de réactivation de compte utilisateur"),
//
//    // TENTATIVES ÉCHOUÉES (NOUVELLES ADDITIONS)
//    ACCOUNT_ACTIVATION_ATTEMPT_FAILED("Tentative d'activation de compte échouée"),
//    ACCOUNT_DEACTIVATION_ATTEMPT_FAILED("Tentative de désactivation de compte échouée"),
//    ACCOUNT_REACTIVATION_ATTEMPT_FAILED("Tentative de réactivation de compte échouée"),
//
//    // GESTION DES TOKENS (NOUVELLES ADDITIONS)
//    ACCOUNT_TOKEN_REVOKED("Révocation de token de compte utilisateur"),
//
//    // PASSWORD
//    PASSWORD_RESET_REQUEST("Demande de réinitialisation de mot de passe"),
//    PASSWORD_REQUEST_TOKEN("Nouvelle demande de réinitialisation de mot de passe"),
//    PASSWORD_RESET_COMPLETE("Réinitialisation de mot de passe effectuée"),
//    PASSWORD_CHANGED("Modification du mot de passe"),
//    PASSWORD_EXPIRED("Expiration du mot de passe"),
//
//    // EMAIL
//    EMAIL_CHANGE_REQUEST("Demande de changement d'adresse e-mail"),
//    EMAIL_VERIFIED("Vérification d'adresse e-mail"),
//    EMAIL_CHANGE_COMPLETE("Changement d'adresse e-mail effectué"),
//    EMAIL_CHANGE_CANCELLED("Annulation du changement d'adresse e-mail"),
//
//    // PROFILE
//    PROFILE_UPDATED("Mise à jour du profil utilisateur"),
//    PROFILE_PICTURE_CHANGED("Changement de photo de profil"),
//
//    // ROLE & PERMISSIONS
//    ROLE_GRANTED("Attribution d'un rôle utilisateur"),
//    ROLE_REVOKED("Révocation d'un rôle utilisateur"),
//    PERMISSION_CHANGED("Modification des permissions utilisateur"),
//
//    // DEVICE
//    DEVICE_REGISTERED("Enregistrement d'un nouvel appareil"),
//    DEVICE_CONFIRMED("Confirmation d'un appareil"),
//    DEVICE_REJECTED("Rejet d'un appareil"),
//    DEVICE_TRUST_LEVEL_CHANGED("Changement du niveau de confiance d'un appareil"),
//    DEVICE_BLACKLISTED("Appareil mis sur liste noire"),
//    DEVICE_WHITELISTED("Appareil retiré de la liste noire"),
//
//    // API
//    API_ACCESS("Accès à l'API"),
//    API_KEY_GENERATED("Génération d'une clé API"),
//    API_KEY_REVOKED("Révocation d'une clé API"),
//
//    // ADMIN
//    ADMIN_USER_CREATED("Création d'utilisateur par un administrateur"),
//    ADMIN_USER_UPDATED("Mise à jour d'utilisateur par un administrateur"),
//    ADMIN_PASSWORD_RESET("Réinitialisation de mot de passe par un administrateur"),
//    ADMIN_FORCE_LOGOUT("Déconnexion forcée par un administrateur"),
//    ADMIN_USER_SEARCH("Recherche d'utilisateurs par un administrateur"),
//    ADMIN_USER_DELETION("Suppression d'utilisateur par un administrateur"),
//    ADMIN_DATA_EXPORT("Export de données par un administrateur"),
//    ADMIN_AUDIT_ACCESS("Accès aux logs d'audit par un administrateur"),
//
//    // SECURITE
//    SECURITY_SUSPICIOUS_ACTIVITY("Activité suspecte détectée"),
//    SECURITY_BRUTE_FORCE_ATTEMPT("Tentative de force brute détectée"),
//    SECURITY_IP_BLOCKED("Adresse IP bloquée"),
//    SECURITY_LOCATION_CHANGE("Changement de localisation détecté"),
//
//    // SYSTEME
//    SYSTEM_ERROR("Erreur système"),
//    SYSTEM_CONFIG_CHANGED("Modification de la configuration système");
//
//    private final String description;
//
//    oldActionLogType(String description) {
//        this.description = description;
//    }
//
//    public String getDescription() {
//        return description;
//    }
//
//    /**
//     * Vérifie si le type d'action appartient à la catégorie spécifiée.
//     *
//     * @param category La catégorie à vérifier (ex: "AUTH", "ACCOUNT", etc.)
//     * @return true si l'action appartient à la catégorie, false sinon
//     */
//    public boolean isCategory(String category) {
//        return this.name().startsWith(category + "_");
//    }
//
//    /**
//     * Récupère la catégorie de l'action.
//     *
//     * @return La catégorie de l'action (ex: "AUTH", "ACCOUNT", etc.)
//     */
//    public String getCategory() {
//        return this.name().split("_")[0];
//    }
//}