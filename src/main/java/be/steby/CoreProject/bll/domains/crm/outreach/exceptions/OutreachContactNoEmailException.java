package be.steby.CoreProject.bll.domains.crm.outreach.exceptions;

import be.steby.CoreProject.bll.domains.crm.outreach.services.CrmOutreachServiceImpl;

/**
 * Thrown when an outreach email cannot be sent because the target contact
 * has no email address on record.
 *
 * <p>Raised by {@link CrmOutreachServiceImpl}
 * after the contact is resolved but before the SMTP dispatch, as a guard against
 * sending to a null or blank email address.</p>
 *
 * <p>HTTP status: 422 (Unprocessable Entity) — the request is structurally valid
 * but cannot be fulfilled given the current state of the contact.</p>
 *
 * @see OutreachDomainException
 */
public class OutreachContactNoEmailException extends OutreachDomainException {

    private static final String ERROR_CODE = "OUTREACH_CONTACT_NO_EMAIL";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    /**
     * Creates a new exception with a message.
     * Default HTTP status is 422 (Unprocessable Entity).
     *
     * @param message the error message
     */
    public OutreachContactNoEmailException(String message) {
        super(message, 422);
    }

    // =========================================================================
    // Static factories
    // =========================================================================

    /**
     * Creates an exception for a contact that has no email address.
     *
     * @param contactPublicId the public UUID of the contact missing an email address
     * @return a new {@code OutreachContactNoEmailException}
     */
    public static OutreachContactNoEmailException forContact(String contactPublicId) {
        return new OutreachContactNoEmailException(
                "Cannot send outreach email: contact " + contactPublicId + " has no email address");
    }
}
