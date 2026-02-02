package be.steby.CoreProject.bll.common.services.validation.email;

import be.steby.CoreProject.bll.domains.emailaddress.models.EmailValidationResult;

public interface EmailPolicyService {

    /**
     * Valide une adresse email selon les règles de politique de l'application.
     *
     * @param email L'adresse email à valider
     * @return Le résultat de la validation avec les erreurs éventuelles
     */
    EmailValidationResult validateEmail(String email);

    /**
     * Validates email format only (regex + length).
     * Does NOT check domain policies or availability.
     *
     * @param email The email address to validate
     * @return The validation result
     */
    EmailValidationResult validateEmailFormat(String email);

    /**
     * Validates email format and domain policies.
     * Does NOT check availability.
     *
     * <p>Useful for contact forms where existing users can submit.</p>
     *
     * @param email The email address to validate
     * @return The validation result
     */
    EmailValidationResult validateEmailFormatAndDomain(String email);


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