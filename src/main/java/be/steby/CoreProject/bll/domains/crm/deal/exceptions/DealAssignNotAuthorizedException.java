package be.steby.CoreProject.bll.domains.crm.deal.exceptions;

/**
 * Thrown when a commercial attempts to assign a deal to another commercial.
 *
 * <p>Only administrators (ADMIN or SUPER_ADMIN) can assign a deal to a user
 * other than themselves. A commercial may only self-assign.
 * Maps to HTTP 403.</p>
 */
public class DealAssignNotAuthorizedException extends DealDomainException {

    private static final String ERROR_CODE = "DEAL_ASSIGN_NOT_AUTHORIZED";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    public DealAssignNotAuthorizedException() {
        super("Only administrators can assign a deal to another commercial", 403);
    }
}
