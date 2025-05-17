package be.steby.CoreProject.bll.domain.password.exceptions;

/**
 * Exception lancée lors de problèmes de validation structurelle des mots de passe,
 * comme un mot de passe manquant ou vide.
 */
public class PasswordValidationException extends PasswordDomainException {

    /**
     * Crée une nouvelle exception avec un message spécifique.
     *
     * @param message Message décrivant le problème de validation
     */
    public PasswordValidationException(String message) {
        super(message);
    }
}