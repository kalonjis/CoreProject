package be.steby.CoreProject.bll.domains.admin.exceptions;

/**
 * Exception thrown when invalid arguments are provided to device operations.
 * Equivalent to IllegalArgumentException but specific to device domain.
 */
public class InvalidAdminArgumentException extends AdminDomainException {

    public InvalidAdminArgumentException(String message) {
        super(message, 400);
    }

    public InvalidAdminArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}