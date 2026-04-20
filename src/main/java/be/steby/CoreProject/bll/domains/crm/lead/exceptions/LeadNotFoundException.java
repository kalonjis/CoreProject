package be.steby.CoreProject.bll.domains.crm.lead.exceptions;

/**
 * Thrown when a lead cannot be found by its public ID.
 *
 * <p>Maps to HTTP 404.</p>
 */
public class LeadNotFoundException extends LeadDomainException {

    private static final String ERROR_CODE = "LEAD_NOT_FOUND";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    public LeadNotFoundException(String publicId) {
        super("Lead not found: " + publicId, 404);
    }
}