package be.steby.CoreProject.bll.common.services.reactivation;

import be.steby.CoreProject.bll.common.models.reactivation.ReactivationEligibility;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.ReactivationPolicy;

/**
 * Service pour gérer les politiques de réactivation des comptes utilisateurs.
 * Service transversal utilisé par plusieurs domaines (user, admin, account).
 */
public interface ReactivationPolicyService {

    /**
     * Vérifie l'éligibilité d'un utilisateur à la réactivation.
     *
     * @param user L'utilisateur à réactiver
     * @param actor L'utilisateur qui effectue la réactivation (peut être null pour auto-reactivation)
     * @return ReactivationEligibility avec le statut et la raison
     */
    ReactivationEligibility checkEligibility(User user, User actor);

    /**
     * Détermine la politique de réactivation à appliquer.
     *
     * @param user L'utilisateur à réactiver
     * @param actor L'utilisateur qui effectue la réactivation
     * @return ReactivationPolicy la politique à appliquer
     */
    ReactivationPolicy determineReactivationPolicy(User user, User actor);

    /**
     * Vérifie si un acteur peut réactiver un utilisateur selon la politique.
     *
     * @param policy La politique de réactivation
     * @param actor L'utilisateur qui effectue la réactivation
     * @param target L'utilisateur à réactiver
     * @return true si autorisé, false sinon
     */
    boolean canReactivateWithPolicy(ReactivationPolicy policy, User actor, User target);
}