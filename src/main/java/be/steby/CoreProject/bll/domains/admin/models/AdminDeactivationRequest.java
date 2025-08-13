package be.steby.CoreProject.bll.domains.admin.models;

import be.steby.CoreProject.bll.domains.admin.exceptions.AdminDeactivationValidationException;

import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import be.steby.CoreProject.dl.enums.admin.deactivation.DeactivationMainCategory;
import be.steby.CoreProject.pl.models.admin.UserDeactivationForm;

/**
 * Représente une demande de désactivation admin dans le domaine métier - Version finale.
 * Utilise les vraies catégories existantes et les nouvelles exceptions.
 * Validation simple uniquement (pas de cohérence catégorie/détails).
 */
public record AdminDeactivationRequest(
        /**
         * ID de l'utilisateur à désactiver
         */
        Long targetUserId,

        /**
         * Catégorie de la désactivation administrative (vraies valeurs)
         */
        AdminDeactivationCategory deactivationCategory,

        /**
         * Détails/commentaires sur la désactivation
         */
        String adminDeactivationDetails

) {
    /**
     * Validation défensive côté BLL - SIMPLE uniquement.
     */
    public AdminDeactivationRequest {
        if (targetUserId == null || targetUserId <= 0) {
            throw AdminDeactivationValidationException.forInvalidTargetUser(
                    targetUserId, "L'ID utilisateur doit être un nombre positif valide");
        }

        if (deactivationCategory == null) {
            throw new AdminDeactivationValidationException(
                    "La catégorie de désactivation ne peut pas être null");
        }

        if (adminDeactivationDetails == null || adminDeactivationDetails.isBlank()) {
            throw AdminDeactivationValidationException.forInvalidDetails(
                    "Les détails de désactivation ne peuvent pas être vides");
        }

        // Validation de longueur uniquement
        String trimmedDetails = adminDeactivationDetails.trim();
        if (trimmedDetails.length() < 10) {
            throw AdminDeactivationValidationException.forInvalidDetails(
                    "Les détails doivent contenir au moins 10 caractères");
        }

        if (trimmedDetails.length() > 500) {
            throw AdminDeactivationValidationException.forInvalidDetails(
                    "Les détails ne peuvent pas dépasser 500 caractères");
        }
    }

    /**
     * Méthode factory pour créer depuis le formulaire PL.
     * Suit le pattern: PasswordChangeRequest.fromForm(form)
     *
     * @param targetUserId ID de l'utilisateur à désactiver
     * @param form Formulaire de la couche présentation
     * @return Nouvelle instance AdminDeactivationRequest
     */
    public static AdminDeactivationRequest fromForm(Long targetUserId, UserDeactivationForm form) {
        return new AdminDeactivationRequest(
                targetUserId,
                form.deactivationCategory(),
                form.adminDeactivationDetails()
        );
    }

    // ===== MÉTHODES BUSINESS AVEC VRAIES CATÉGORIES =====

    /**
     * Indique si cette désactivation nécessite des permissions Super Admin.
     * Basé sur les vraies catégories existantes.
     */
    public boolean requiresSuperAdminPermissions() {
        return deactivationCategory.requiresSuperAdminApproval();
    }

    /**
     * Indique si cette désactivation est liée à la sécurité.
     * Utilise les vraies catégories SECURITY_RISK_*.
     */
    public boolean isSecurityRelated() {
        return deactivationCategory.getMainCategory() == DeactivationMainCategory.SECURITY_RISK;
    }

    /**
     * Indique si cette désactivation est un bannissement.
     * Utilise les vraies catégories BANNED_*.
     */
    public boolean isBannishment() {
        return deactivationCategory.getMainCategory() == DeactivationMainCategory.BANNED;
    }

    /**
     * Indique si cette désactivation est liée à une question légale.
     * Utilise les vraies catégories LEGAL_*.
     */
    public boolean isLegalRelated() {
        return deactivationCategory.getMainCategory() == DeactivationMainCategory.LEGAL;
    }

    /**
     * Indique si cette désactivation est temporaire/maintenance.
     * Utilise les vraies catégories MAINTENANCE_*.
     */
    public boolean isTemporary() {
        return deactivationCategory.getMainCategory() == DeactivationMainCategory.MAINTENANCE;
    }

    /**
     * Les sessions sont invalidées selon la catégorie.
     * Business rule intelligente basée sur les vraies catégories.
     */
    public boolean invalidateActiveSessions() {
        return deactivationCategory.requiresSessionInvalidation();
    }

    /**
     * Indique si la réactivation automatique est possible.
     */
    public boolean allowsReactivation() {
        return deactivationCategory.allowsReactivation();
    }

    /**
     * Obtient la priorité de traitement selon la catégorie.
     */
    public DeactivationPriority getPriority() {
        return switch (deactivationCategory.getSeverityLevel()) {
            case 5 -> DeactivationPriority.CRITICAL;  // BANNED_*, LEGAL_*
            case 4 -> DeactivationPriority.HIGH;      // SECURITY_RISK_*, TOS_VIOLATION_FRAUD/IMPERSONATION
            case 3 -> DeactivationPriority.MEDIUM;    // TOS_VIOLATION_* autres
            case 2 -> DeactivationPriority.LOW;       // TOS_VIOLATION_SPAM, ADMINISTRATIVE
            default -> DeactivationPriority.LOW;      // MAINTENANCE_*
        };
    }

    /**
     * Indique si des notifications spéciales sont nécessaires.
     */
    public boolean requiresSpecialNotifications() {
        return switch (deactivationCategory.getMainCategory()) {
            case SECURITY_RISK -> true;  // Alerte équipe sécurité
            case LEGAL -> true;          // Alerte équipe légale
            case BANNED -> deactivationCategory == AdminDeactivationCategory.BANNED_HATE_SPEECH; // Alerte spéciale
            default -> false;
        };
    }

    /**
     * Obtient la durée de rétention des logs.
     */
    public int getLogRetentionDays() {
        return deactivationCategory.getLogRetentionDays();
    }

    /**
     * Indique si cette désactivation nécessite une explication détaillée.
     */
    public boolean requiresDetailedExplanation() {
        return deactivationCategory.requiresDetailedExplanation();
    }

    public enum DeactivationPriority {
        CRITICAL, HIGH, MEDIUM, LOW
    }
}