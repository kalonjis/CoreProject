package be.steby.CoreProject.bll.domains.lead.exceptions;

/**
 * Exception thrown when honeypot field is filled (bot detected).
 *
 * <p>This exception should be handled silently - return a fake success
 * response to avoid informing bots that they were detected.</p>
 */
public class HoneypotDetectedException extends LeadDomainException {

    public HoneypotDetectedException(String message) {
        super(message);
    }
}