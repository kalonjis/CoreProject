package be.steby.CoreProject.bll.domains.crm.lead.exceptions;

/**
 * Thrown when an operation is attempted on a lead that has already been converted.
 *
 * <p>A converted lead is terminal — no further status transitions are allowed.
 * Maps to HTTP 409.</p>
 */
public class LeadAlreadyConvertedException extends LeadDomainException {

    private static final String ERROR_CODE = "LEAD_ALREADY_CONVERTED";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    public LeadAlreadyConvertedException(String publicId) {
        super("Lead has already been converted: " + publicId, 409);
    }
}