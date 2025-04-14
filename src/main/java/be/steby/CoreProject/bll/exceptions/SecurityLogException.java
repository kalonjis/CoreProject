package be.steby.CoreProject.bll.exceptions;

/**
 * Exception dédiée aux erreurs liées à la journalisation de sécurité.
 * Permet au ControllerAdvisor de traiter spécifiquement ces types d'erreurs.
 */
public class SecurityLogException extends CoreProjectException {

    /**
     * Construit une nouvelle SecurityLogException avec le message spécifié.
     * Le code de statut HTTP est défini à 500 (Erreur serveur interne) par défaut.
     *
     * @param message Le message d'erreur détaillé
     */
    public SecurityLogException(String message) {
        super(message, 500);
    }

    /**
     * Construit une nouvelle SecurityLogException avec le message et le code de statut spécifiés.
     *
     * @param message Le message d'erreur détaillé
     * @param status Le code de statut HTTP à retourner
     */
    public SecurityLogException(String message, int status) {
        super(message, status);
    }

    /**
     * Construit une nouvelle SecurityLogException encapsulant une autre exception.
     * Utile pour conserver la chaîne d'exceptions tout en adaptant l'exception
     * à notre système unifié de gestion des erreurs.
     *
     * @param message Le message d'erreur détaillé
     * @param cause L'exception d'origine ayant causé celle-ci
     */
    public SecurityLogException(String message, Throwable cause) {
        super(message, 500);
        initCause(cause);
    }

    /**
     * Construit une nouvelle SecurityLogException encapsulant une autre exception
     * avec un code de statut HTTP personnalisé.
     *
     * @param message Le message d'erreur détaillé
     * @param status Le code de statut HTTP à retourner
     * @param cause L'exception d'origine ayant causé celle-ci
     */
    public SecurityLogException(String message, int status, Throwable cause) {
        super(message, status);
        initCause(cause);
    }
}