package be.steby.CoreProject.bll.domains.gdpr.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a GDPR export operation cannot proceed due to a business rule violation.
 *
 * <p>Examples:
 * <ul>
 *   <li>A request is already active for this user</li>
 *   <li>Cooldown period has not elapsed</li>
 *   <li>Token is invalid, expired, or already used</li>
 * </ul>
 */
public class GdprExportException extends GdprDomainException {

    public GdprExportException(String message) {
        super(message, 400);
    }
}