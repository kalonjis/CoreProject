package be.steby.CoreProject.bll.domains.password.exceptions;

/**
 * Exception thrown when a user attempts to define a password
 * but already has one defined.
 */
public class PasswordAlreadyDefinedException extends PasswordDomainException {
    public PasswordAlreadyDefinedException(String message) {
        super(message, 400);
    }
}