package be.steby.CoreProject.dl.enums;

/**
 * Enumération des catégories spécifiques pour les désactivations administratives.
 * Utilisée en complément de DeactivationReason.ADMIN_DECISION pour apporter plus de granularité.
 */
public enum AdminDeactivationCategory {

    // Violations comportementales
    BANNED_DOXXING("Bannissement - Divulgation d'informations privées",
            "L'utilisateur a divulgué des informations personnelles d'autres utilisateurs"),

    BANNED_INAPPROPRIATE_BEHAVIOR("Bannissement - Comportement inapproprié",
            "L'utilisateur a eu un comportement inapproprié envers d'autres membres de la communauté"),

    BANNED_HARASSMENT("Bannissement - Harcèlement",
            "L'utilisateur a harcelé ou intimidé d'autres utilisateurs"),

    BANNED_HATE_SPEECH("Bannissement - Discours de haine",
            "L'utilisateur a tenu des propos haineux ou discriminatoires"),

    // Violations des conditions d'utilisation
    S_VIOLATION_COMMERCIAL_ABUSE("Violation TOS - Abus commercial",
            "L'utilisateur utilise la plateforme à des fins commerciales non autorisées"),


    TOS_VIOLATION_SPAM("Violation TOS - Spam",
            "L'utilisateur a envoyé du contenu spam ou des messages non sollicités"),

    TOS_VIOLATION_FRAUD("Violation TOS - Fraude",
            "L'utilisateur a tenté de frauder ou tromper d'autres utilisateurs"),

    TOS_VIOLATION_IMPERSONATION("Violation TOS - Usurpation d'identité",
            "L'utilisateur a usurpé l'identité d'une autre personne"),

    TOS_VIOLATION_MULTIPLE_ACCOUNTS("Violation TOS - Comptes multiples",
            "L'utilisateur a créé plusieurs comptes en violation des règles"),

    TOS_VIOLATION_UNDERAGE("Violation TOS - Utilisateur mineur",
            "L'utilisateur est mineur et ne respecte pas l'âge minimum requis"),


    // Risques de sécurité
    SECURITY_RISK_DATA_BREACH("Risque sécuritaire - Fuite de données",
            "Le compte a été impliqué dans une fuite de données"),

    SECURITY_RISK_COMPROMISED("Risque sécuritaire - Compte compromis",
            "Le compte présente des signes de compromission"),

    SECURITY_RISK_SUSPICIOUS_ACTIVITY("Risque sécuritaire - Activité suspecte",
            "Des activités suspectes ont été détectées sur ce compte"),

    SECURITY_RISK_MALWARE("Risque sécuritaire - Malware",
            "Le compte a été utilisé pour distribuer des logiciels malveillants"),

    // Problèmes légaux
    LEGAL_REQUEST("Demande légale",
            "Désactivation suite à une demande des autorités légales"),

    LEGAL_COPYRIGHT("Violation de droits d'auteur",
            "L'utilisateur a violé des droits d'auteur de façon répétée"),

    LEGAL_MINOR_SAFETY("Protection des mineurs",
            "Désactivation pour protéger la sécurité des mineurs"),

    // Maintenance et nettoyage
    MAINTENANCE_INACTIVE("Maintenance - Compte inactif",
            "Compte inactif depuis une période prolongée"),

    MAINTENANCE_DATA_CLEANUP("Maintenance - Nettoyage des données",
            "Désactivation dans le cadre du nettoyage périodique"),

    MAINTENANCE_DUPLICATE("Maintenance - Compte en doublon",
            "Compte identifié comme doublon d'un compte existant"),

    // Cas spéciaux
    ADMIN_ERROR("Erreur administrative",
            "Désactivation accidentelle par erreur administrative"),

    REQUESTED_BY_USER("Demandé par l'utilisateur",
            "Désactivation effectuée à la demande explicite de l'utilisateur mais traitée par un admin"),

    OTHER_ADMIN_REASON("Autre raison administrative",
            "Autre raison administrative non listée ci-dessus");

    private final String displayName;
    private final String description;

    AdminDeactivationCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

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
     * Détermine si cette catégorie permet la réactivation automatique
     */
    public boolean allowsReactivation() {
        return switch (this) {
            case MAINTENANCE_INACTIVE, MAINTENANCE_DATA_CLEANUP,
                 ADMIN_ERROR, REQUESTED_BY_USER -> true;
            case BANNED_INAPPROPRIATE_BEHAVIOR, BANNED_HARASSMENT, BANNED_HATE_SPEECH,
                 TOS_VIOLATION_FRAUD, LEGAL_REQUEST, LEGAL_COPYRIGHT,
                 LEGAL_MINOR_SAFETY -> false;
            default -> false; // Par défaut, nécessite une intervention manuelle
        };
    }

    /**
     * Retourne le niveau de sévérité de la catégorie (1-5, 5 étant le plus sévère)
     */
    public int getSeverityLevel() {
        return switch (this) {
            case MAINTENANCE_INACTIVE, MAINTENANCE_DATA_CLEANUP,
                 ADMIN_ERROR, REQUESTED_BY_USER -> 1;
            case MAINTENANCE_DUPLICATE, TOS_VIOLATION_MULTIPLE_ACCOUNTS -> 2;
            case TOS_VIOLATION_SPAM, SECURITY_RISK_COMPROMISED -> 3;
            case BANNED_INAPPROPRIATE_BEHAVIOR, TOS_VIOLATION_IMPERSONATION,
                 SECURITY_RISK_SUSPICIOUS_ACTIVITY, SECURITY_RISK_MALWARE -> 4;
            case BANNED_HARASSMENT, BANNED_HATE_SPEECH, TOS_VIOLATION_FRAUD,
                 LEGAL_REQUEST, LEGAL_COPYRIGHT, LEGAL_MINOR_SAFETY -> 5;
            default -> 3;
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
     * Détermine si cette catégorie nécessite une approbation de niveau supérieur
     */
    public boolean requiresSuperAdminApproval() {
        return switch (this) {
            case LEGAL_REQUEST, LEGAL_MINOR_SAFETY,
                 BANNED_HATE_SPEECH -> true;
            default -> false;
        };
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