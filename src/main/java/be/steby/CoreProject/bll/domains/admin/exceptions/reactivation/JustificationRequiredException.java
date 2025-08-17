package be.steby.CoreProject.bll.domains.admin.exceptions.reactivation;


import be.steby.CoreProject.bll.domains.admin.exceptions.AdminDomainException;

/**
 * Exception thrown when justification is required but missing/insufficient.
 * SIMPLE - just message and status, like the project pattern.
 */
public class JustificationRequiredException extends AdminDomainException {

    public JustificationRequiredException(String message) {
        super(message, 400); // Bad Request
    }

    public JustificationRequiredException(String message, int status) {
        super(message, status);
    }

}