package be.steby.CoreProject.bll.domains.contact.exceptions;

/**
 * Thrown when a commercial attempts to assign a contact to another commercial.
 *
 * <p>Only administrators (ADMIN or SUPER_ADMIN) can assign a contact to a user
 * other than themselves. A commercial may only self-assign.
 * Maps to HTTP 403.</p>
 */
public class ContactAssignNotAuthorizedException extends ContactDomainException {

    private static final String ERROR_CODE = "CONTACT_ASSIGN_NOT_AUTHORIZED";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    public ContactAssignNotAuthorizedException() {
        super("Only administrators can assign a contact to another commercial", 403);
    }
}
