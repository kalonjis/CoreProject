package be.steby.CoreProject.dl.enums.admin.deactivation;

/**
 * Catégories principales pour les désactivations administratives.
 * Sert à organiser les AdminDeactivationCategory en groupes logiques.
 */
public enum DeactivationMainCategory {

    BANNED("Bannissements",
            "Comportements inacceptables nécessitant un bannissement"),

    TOS_VIOLATION("Violations TOS",
            "Violations des conditions d'utilisation"),

    SECURITY_RISK("Risques sécuritaires",
            "Menaces à la sécurité de la plateforme"),

    LEGAL("Problèmes légaux",
            "Questions légales et conformité"),

    MAINTENANCE("Maintenance",
            "Opérations de maintenance et nettoyage"),

    ADMINISTRATIVE("Administratif",
            "Cas spéciaux et erreurs administratives");

    private final String displayName;
    private final String description;

    DeactivationMainCategory(String displayName, String description) {
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
     * Retourne la sévérité par défaut pour cette catégorie principale
     */
    public int getDefaultSeverityLevel() {
        return switch (this) {
            case MAINTENANCE -> 1;
            case ADMINISTRATIVE -> 2;
            case TOS_VIOLATION -> 3;
            case SECURITY_RISK -> 4;
            case BANNED, LEGAL -> 5;
        };
    }

    /**
     * Indique si cette catégorie permet généralement la réactivation
     */
    public boolean generallyAllowsReactivation() {
        return switch (this) {
            case MAINTENANCE, ADMINISTRATIVE -> true;
            case BANNED, LEGAL -> false;
            case TOS_VIOLATION, SECURITY_RISK -> false; // Dépend de la sous-catégorie
        };
    }

    /**
     * Indique si cette catégorie nécessite généralement l'invalidation des sessions
     */
    public boolean generallyRequiresSessionInvalidation() {
        return switch (this) {
            case SECURITY_RISK, BANNED -> true;
            case TOS_VIOLATION -> false; // Dépend de la sévérité
            case LEGAL, MAINTENANCE, ADMINISTRATIVE -> false;
        };
    }

    /**
     * Retourne les catégories par ordre de sévérité
     */
    public static DeactivationMainCategory[] getBySeverityOrder() {
        return new DeactivationMainCategory[] {
                MAINTENANCE,
                ADMINISTRATIVE,
                TOS_VIOLATION,
                SECURITY_RISK,
                BANNED,
                LEGAL
        };
    }
}