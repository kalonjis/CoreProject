package be.steby.CoreProject.bll.domains.outreach.exceptions;

import be.steby.CoreProject.bll.domains.crm.contact.exceptions.ContactNotFoundException;

/**
 * Thrown when the target contact of a CRM outreach send cannot be resolved.
 *
 * <p>Raised by {@link be.steby.CoreProject.bll.domains.outreach.services.CrmOutreachServiceImpl}
 * when no contact exists for the provided public UUID. This is distinct from
 * {@link ContactNotFoundException}:
 * the failure is reported from the perspective of the outreach domain, not the
 * contact CRUD domain.</p>
 *
 * <p>HTTP status: 404 (Not Found)</p>
 *
 * @see OutreachDomainException
 */
public class OutreachContactNotFoundException extends OutreachDomainException {

    private static final String ERROR_CODE = "OUTREACH_CONTACT_NOT_FOUND";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    /**
     * Creates a new exception with a message.
     * Default HTTP status is 404 (Not Found).
     *
     * @param message the error message
     */
    public OutreachContactNotFoundException(String message) {
        super(message, 404);
    }

    /**
     * Creates a new exception with a message and underlying cause.
     * Default HTTP status is 404 (Not Found).
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public OutreachContactNotFoundException(String message, Throwable cause) {
        super(message, 404, cause);
    }

    // =========================================================================
    // Static factories
    // =========================================================================

    /**
     * Creates an exception for a contact not found by its public UUID.
     *
     * @param publicId the public UUID that yielded no result
     * @return a new {@code OutreachContactNotFoundException}
     */
    public static OutreachContactNotFoundException byPublicId(String publicId) {
        return new OutreachContactNotFoundException(
                "Outreach target contact not found with publicId: " + publicId);
    }
}
