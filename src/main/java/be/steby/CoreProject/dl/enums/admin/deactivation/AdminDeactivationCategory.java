package be.steby.CoreProject.dl.enums.admin.deactivation;

/**
 * Enumération des catégories spécifiques pour les désactivations administratives.
 * Structure organisée en catégories principales et sous-catégories.
 */
public enum AdminDeactivationCategory {

    // ===== CATÉGORIE : BANNISSEMENTS =====
    BANNED_DOXXING("Bannissement - Divulgation d'informations privées",
            "L'utilisateur a divulgué des informations personnelles d'autres utilisateurs",
            DeactivationMainCategory.BANNED),

    BANNED_INAPPROPRIATE_BEHAVIOR("Bannissement - Comportement inapproprié",
            "L'utilisateur a eu un comportement inapproprié envers d'autres membres",
            DeactivationMainCategory.BANNED),

    BANNED_HARASSMENT("Bannissement - Harcèlement",
            "L'utilisateur a harcelé ou intimidé d'autres utilisateurs",
            DeactivationMainCategory.BANNED),

    BANNED_HATE_SPEECH("Bannissement - Discours de haine",
            "L'utilisateur a tenu des propos haineux ou discriminatoires",
            DeactivationMainCategory.BANNED),

    // ===== CATÉGORIE : VIOLATIONS CONDITIONS D'UTILISATION =====
    TOS_VIOLATION_COMMERCIAL_ABUSE("Violation TOS - Abus commercial",
            "L'utilisateur utilise la plateforme à des fins commerciales non autorisées",
            DeactivationMainCategory.TOS_VIOLATION),

    TOS_VIOLATION_SPAM("Violation TOS - Spam",
            "L'utilisateur a envoyé du contenu spam ou des messages non sollicités",
            DeactivationMainCategory.TOS_VIOLATION),

    TOS_VIOLATION_FRAUD("Violation TOS - Fraude",
            "L'utilisateur a tenté de frauder ou tromper d'autres utilisateurs",
            DeactivationMainCategory.TOS_VIOLATION),

    TOS_VIOLATION_IMPERSONATION("Violation TOS - Usurpation d'identité",
            "L'utilisateur a usurpé l'identité d'une autre personne",
            DeactivationMainCategory.TOS_VIOLATION),

    TOS_VIOLATION_MULTIPLE_ACCOUNTS("Violation TOS - Comptes multiples",
            "L'utilisateur a créé plusieurs comptes en violation des règles",
            DeactivationMainCategory.TOS_VIOLATION),

    TOS_VIOLATION_UNDERAGE("Violation TOS - Utilisateur mineur",
            "L'utilisateur est mineur et ne respecte pas l'âge minimum requis",
            DeactivationMainCategory.TOS_VIOLATION),

    // ===== CATÉGORIE : RISQUES DE SÉCURITÉ =====
    SECURITY_RISK_DATA_BREACH("Risque sécuritaire - Fuite de données",
            "Le compte a été impliqué dans une fuite de données",
            DeactivationMainCategory.SECURITY_RISK),

    SECURITY_RISK_COMPROMISED("Risque sécuritaire - Compte compromis",
            "Le compte présente des signes de compromission",
            DeactivationMainCategory.SECURITY_RISK),

    SECURITY_RISK_SUSPICIOUS_ACTIVITY("Risque sécuritaire - Activité suspecte",
            "Des activités suspectes ont été détectées sur ce compte",
            DeactivationMainCategory.SECURITY_RISK),

    SECURITY_RISK_MALWARE("Risque sécuritaire - Malware",
            "Le compte a été utilisé pour distribuer des logiciels malveillants",
            DeactivationMainCategory.SECURITY_RISK),

    // ===== CATÉGORIE : PROBLÈMES LÉGAUX =====
    LEGAL_REQUEST("Demande légale",
            "Désactivation suite à une demande des autorités légales",
            DeactivationMainCategory.LEGAL),

    LEGAL_COPYRIGHT("Violation de droits d'auteur",
            "L'utilisateur a violé des droits d'auteur de façon répétée",
            DeactivationMainCategory.LEGAL),

    LEGAL_MINOR_SAFETY("Protection des mineurs",
            "Désactivation pour protéger la sécurité des mineurs",
            DeactivationMainCategory.LEGAL),

    // ===== CATÉGORIE : MAINTENANCE =====
    MAINTENANCE_INACTIVE("Maintenance - Compte inactif",
            "Compte inactif depuis une période prolongée",
            DeactivationMainCategory.MAINTENANCE),

    MAINTENANCE_DATA_CLEANUP("Maintenance - Nettoyage des données",
            "Désactivation dans le cadre du nettoyage périodique",
            DeactivationMainCategory.MAINTENANCE),

    MAINTENANCE_DUPLICATE("Maintenance - Compte en doublon",
            "Compte identifié comme doublon d'un compte existant",
            DeactivationMainCategory.MAINTENANCE),

    // ===== CATÉGORIE : CAS SPÉCIAUX =====
    ADMIN_ERROR("Erreur administrative",
            "Désactivation accidentelle par erreur administrative",
            DeactivationMainCategory.ADMINISTRATIVE),

    REQUESTED_BY_USER("Demandé par l'utilisateur",
            "Désactivation effectuée à la demande explicite de l'utilisateur",
            DeactivationMainCategory.ADMINISTRATIVE),

    OTHER_ADMIN_REASON("Autre raison administrative",
            "Autre raison administrative non listée ci-dessus",
            DeactivationMainCategory.ADMINISTRATIVE);

    private final String displayName;
    private final String description;
    private final DeactivationMainCategory mainCategory;

    AdminDeactivationCategory(String displayName, String description, DeactivationMainCategory mainCategory) {
        this.displayName = displayName;
        this.description = description;
        this.mainCategory = mainCategory;
    }

    // Getters existants
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public DeactivationMainCategory getMainCategory() { return mainCategory; }

    // ===== NOUVELLES MÉTHODES AVEC STRUCTURE CATÉGORIES =====

    /**
     * Retourne toutes les sous-catégories d'une catégorie principale
     */
    public static AdminDeactivationCategory[] getByMainCategory(DeactivationMainCategory mainCategory) {
        return java.util.Arrays.stream(values())
                .filter(category -> category.getMainCategory() == mainCategory)
                .toArray(AdminDeactivationCategory[]::new);
    }

    /**
     * Retourne les catégories qui nécessitent une invalidation de sessions
     */
    public boolean requiresSessionInvalidation() {
        return switch (mainCategory) {
            case SECURITY_RISK, BANNED -> true;
            case TOS_VIOLATION -> getSeverityLevel() >= 4;
            default -> false;
        };
    }

    /**
     * Retourne les catégories qui nécessitent une validation Super Admin
     */
    public boolean requiresSuperAdminApproval() {
        return switch (this) {
            case LEGAL_REQUEST, LEGAL_MINOR_SAFETY, BANNED_HATE_SPEECH -> true;
            default -> mainCategory == DeactivationMainCategory.LEGAL;
        };
    }

    // Méthodes existantes adaptées
    public boolean allowsReactivation() {
        return switch (mainCategory) {
            case MAINTENANCE, ADMINISTRATIVE -> true;
            case BANNED, LEGAL -> false;
            case SECURITY_RISK -> this == SECURITY_RISK_COMPROMISED; // Après sécurisation
            case TOS_VIOLATION -> getSeverityLevel() <= 2;
        };
    }

    public int getSeverityLevel() {
        return switch (mainCategory) {
            case MAINTENANCE -> 1;
            case ADMINISTRATIVE -> 2;
            case TOS_VIOLATION -> switch (this) {
                case TOS_VIOLATION_SPAM, TOS_VIOLATION_MULTIPLE_ACCOUNTS -> 2;
                case TOS_VIOLATION_COMMERCIAL_ABUSE, TOS_VIOLATION_UNDERAGE -> 3;
                case TOS_VIOLATION_FRAUD, TOS_VIOLATION_IMPERSONATION -> 4;
                default -> 3;
            };
            case SECURITY_RISK -> 4;
            case BANNED, LEGAL -> 5;
        };
    }

    // ===== MÉTHODES EXISTANTES CONSERVÉES =====

    /**
     * Détermine si cette catégorie nécessite une explication détaillée obligatoire
     */
    public boolean requiresDetailedExplanation() {
        return switch (this) {
            case BANNED_INAPPROPRIATE_BEHAVIOR, BANNED_HARASSMENT, BANNED_HATE_SPEECH,
                 TOS_VIOLATION_FRAUD, TOS_VIOLATION_IMPERSONATION,
                 SECURITY_RISK_SUSPICIOUS_ACTIVITY, LEGAL_REQUEST,
                 ADMIN_ERROR, OTHER_ADMIN_REASON -> true;
            default -> false;
        };
    }

    /**
     * Retourne les catégories par niveau de sévérité
     */
    public static AdminDeactivationCategory[] getBySeverityLevel(int level) {
        return java.util.Arrays.stream(values())
                .filter(category -> category.getSeverityLevel() == level)
                .toArray(AdminDeactivationCategory[]::new);
    }

    /**
     * Retourne les catégories qui permettent la réactivation
     */
    public static AdminDeactivationCategory[] getReactivableCategories() {
        return java.util.Arrays.stream(values())
                .filter(AdminDeactivationCategory::allowsReactivation)
                .toArray(AdminDeactivationCategory[]::new);
    }

    /**
     * Détermine la durée de rétention des logs pour cette catégorie
     */
    public int getLogRetentionDays() {
        return switch (this) {
            case LEGAL_REQUEST, LEGAL_COPYRIGHT, LEGAL_MINOR_SAFETY -> 2555; // 7 ans
            case BANNED_HARASSMENT, BANNED_HATE_SPEECH -> 1095; // 3 ans
            case SECURITY_RISK_MALWARE, TOS_VIOLATION_FRAUD -> 730; // 2 ans
            default -> 365; // 1 an
        };
    }
}