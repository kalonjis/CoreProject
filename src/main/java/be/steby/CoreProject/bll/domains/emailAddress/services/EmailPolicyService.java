package be.steby.CoreProject.bll.domains.emailAddress.services;

import be.steby.CoreProject.bll.domains.emailAddress.models.EmailValidationResult;

public interface EmailPolicyService {

    /**
     * Valide une adresse email selon les règles de politique de l'application.
     *
     * @param email L'adresse email à valider
     * @return Le résultat de la validation avec les erreurs éventuelles
     */
    EmailValidationResult validateEmail(String email);

    /**
     * Vérifie si une adresse email est disponible (non utilisée par un autre utilisateur).
     *
     * @param email L'adresse email à vérifier
     * @return true si l'email est disponible, false sinon
     */
    boolean isEmailAvailable(String email);

    /**
     * Vérifie si une adresse email est dans la liste des domaines autorisés.
     *
     * @param email L'adresse email à vérifier
     * @return true si le domaine est autorisé, false sinon
     */
    boolean isEmailDomainAllowed(String email);

    /**
     * Vérifie si une adresse email est dans la liste noire des domaines interdits.
     *
     * @param email L'adresse email à vérifier
     * @return true si le domaine est interdit, false sinon
     */
    boolean isEmailDomainBlacklisted(String email);
}