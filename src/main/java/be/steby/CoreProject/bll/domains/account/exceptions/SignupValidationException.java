package be.steby.CoreProject.bll.domains.account.exceptions;

/**
 * Exception thrown when signup validation fails.
 * Contains detailed validation errors for client feedback.
 */
public class SignupValidationException extends AccountDomainException {

    public SignupValidationException(String message) {
        super(message);
    }

    public SignupValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
