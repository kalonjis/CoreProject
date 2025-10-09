package be.steby.CoreProject.bll.domains.admin.exceptions;

/**
 * Exception for admin domain permission violations.
 * Specific to administrative operations and role hierarchy.
 */
public class AdminPermissionException extends AdminDomainException {

    public AdminPermissionException(String message) {
        super(message);
    }

    public AdminPermissionException(String message, int status) {
        super(message, status);
    }

    public AdminPermissionException(String message, Throwable cause) {
        super(message, cause);
    }
}