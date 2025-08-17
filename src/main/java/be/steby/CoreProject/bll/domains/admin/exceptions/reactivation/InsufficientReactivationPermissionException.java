package be.steby.CoreProject.bll.domains.admin.exceptions.reactivation;

import be.steby.CoreProject.bll.domains.admin.exceptions.AdminDomainException;

/**
 * Exception thrown when user lacks permissions to reactivate.
 * SIMPLE - just message and status, like the project pattern.
 */
public class InsufficientReactivationPermissionException extends AdminDomainException {

    public InsufficientReactivationPermissionException(String message) {
        super(message, 403); // Forbidden
    }

    public InsufficientReactivationPermissionException(String message, int status) {
        super(message, status);
    }

}