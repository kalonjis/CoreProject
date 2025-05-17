package be.steby.CoreProject.bll.domain.password.exceptions;

/**
 * Exception lancée lorsque le mot de passe actuel fourni ne correspond pas
 * au mot de passe enregistré de l'utilisateur.
 */
public class PasswordMismatchException extends PasswordDomainException {

  /**
   * Crée une nouvelle exception avec un message par défaut.
   */
  public PasswordMismatchException() {
    super("Le mot de passe actuel est incorrect");
  }

  /**
   * Crée une nouvelle exception avec un message personnalisé.
   *
   * @param message Message personnalisé
   */
  public PasswordMismatchException(String message) {
    super(message);
  }
}