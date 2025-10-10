package be.steby.CoreProject.bll.domains.admin.exceptions;

/**
 * Exception for invalid arguments in admin domain operations.
 * Thrown when business validation fails in admin domain.
 */
public class AdminValidationException extends AdminDomainException {

    public AdminValidationException(String message) {
        super(message, 406);
    }

    public AdminValidationException(String message, int status) {
        super(message, status);
    }

    public AdminValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}