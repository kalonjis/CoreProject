package be.steby.CoreProject.bll.domains.crm.lead.exceptions;

/**
 * Thrown when an operation is attempted on a lead that has already been rejected.
 *
 * <p>A rejected lead is terminal — no further status transitions are allowed.
 * Maps to HTTP 409.</p>
 */
public class LeadAlreadyRejectedException extends LeadDomainException {

    private static final String ERROR_CODE = "LEAD_ALREADY_REJECTED";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    public LeadAlreadyRejectedException(String publicId) {
        super("Lead has already been rejected: " + publicId, 409);
    }
}