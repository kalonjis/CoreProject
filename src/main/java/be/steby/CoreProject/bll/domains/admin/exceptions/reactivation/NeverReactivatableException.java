package be.steby.CoreProject.bll.domains.admin.exceptions.reactivation;

import be.steby.CoreProject.bll.domains.admin.exceptions.AdminDomainException;

/**
 * Exception thrown when attempting to reactivate a category that can never be reactivated.
 */
public class NeverReactivatableException extends AdminDomainException {

    public NeverReactivatableException(String message) {
        super(message, 422); // Unprocessable Entity
    }

    public NeverReactivatableException(String message, int status) {
        super(message, status);
    }
}