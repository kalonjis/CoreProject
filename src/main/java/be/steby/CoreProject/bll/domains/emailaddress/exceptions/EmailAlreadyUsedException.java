package be.steby.CoreProject.bll.domains.emailaddress.exceptions;



/**
 * Exception lancée lorsqu'une adresse email est déjà utilisée par un autre utilisateur.
 */
public class EmailAlreadyUsedException extends EmailDomainException {

    /**
     * Crée une nouvelle exception avec un message spécifique.
     *
     * @param email L'adresse email qui est déjà utilisée
     */
    public EmailAlreadyUsedException(String email) {
        super("L'adresse email " + email + " est déjà utilisée par un autre utilisateur", 409);
    }

    /**
     * Crée une nouvelle exception avec un message personnalisé.
     *
     * @param message Message personnalisé
     */
    public EmailAlreadyUsedException(String message, int status) {
        super(message, status);
    }
}
