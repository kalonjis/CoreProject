package be.steby.CoreProject.bll.domains.admin.exceptions;

/**
 * Exception thrown when invalid arguments are provided to admin operations.
 * Equivalent to IllegalArgumentException but specific to admin domain.
 */
public class InvalidAdminArgumentException extends AdminDomainException {

    public InvalidAdminArgumentException(String message) {
        super(message, 400);
    }

    public InvalidAdminArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}