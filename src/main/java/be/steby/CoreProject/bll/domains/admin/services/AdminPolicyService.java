package be.steby.CoreProject.bll.domains.admin.services;

import be.steby.CoreProject.bll.domains.admin.models.AdminDeactivationRequest;
import be.steby.CoreProject.bll.domains.admin.models.AdminUserCreationRequest;
import be.steby.CoreProject.bll.domains.admin.models.AdminValidationResult;

/**
 * Interface de service pour la validation des politiques administratives.
 * Version hybride : se concentre sur les validations où AdminPolicyService apporte une vraie valeur ajoutée.
 */
public interface AdminPolicyService {

    /**
     * Valide une demande de création d'utilisateur par un administrateur.
     * Validation complexe : email, username, rôles, cohérence, etc.
     *
     * @param request La demande de création à valider
     * @return AdminValidationResult contenant le statut et les erreurs éventuelles
     */
    AdminValidationResult validateUserCreation(AdminUserCreationRequest request);

    /**
     * Valide uniquement les détails de désactivation (catégorie + commentaires).
     * Les permissions sont gérées par UserPermissionService pour plus de précision.
     *
     * @param request La demande de désactivation à valider
     * @return AdminValidationResult contenant le statut et les erreurs éventuelles
     */
    AdminValidationResult validateDeactivationDetails(AdminDeactivationRequest request);

    // Les autres méthodes peuvent être supprimées car UserPermissionService est plus précis
}