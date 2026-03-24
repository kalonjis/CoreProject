package be.steby.CoreProject.bll.domains.lead.exceptions;

/**
 * Thrown when a commercial attempts to assign a lead to another commercial.
 *
 * <p>Only administrators (ADMIN or SUPER_ADMIN) can assign a lead to a user
 * other than themselves. A commercial may only self-assign.
 * Maps to HTTP 403.</p>
 */
public class LeadAssignNotAuthorizedException extends LeadDomainException {

    private static final String ERROR_CODE = "LEAD_ASSIGN_NOT_AUTHORIZED";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    public LeadAssignNotAuthorizedException() {
        super("Only administrators can assign a lead to another commercial", 403);
    }
}
