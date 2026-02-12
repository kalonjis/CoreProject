package be.steby.CoreProject.bll.domains.notification.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a notification cannot be found.
 *
 * <p>This exception is thrown when attempting to access a notification
 * by its public ID and either:</p>
 * <ul>
 *   <li>The notification does not exist</li>
 *   <li>The notification exists but belongs to a different user</li>
 * </ul>
 *
 * <p>Returns HTTP 404 Not Found when thrown from a controller.</p>
 *
 * @see be.steby.CoreProject.bll.domains.notification.services.NotificationService
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotificationNotFoundException extends RuntimeException {

    private final String publicId;

    /**
     * Creates exception for a missing notification.
     *
     * @param publicId the notification's public ID that was not found
     */
    public NotificationNotFoundException(String publicId) {
        super("Notification not found: " + publicId);
        this.publicId = publicId;
    }

    /**
     * Creates exception with custom message.
     *
     * @param publicId the notification's public ID
     * @param message  custom error message
     */
    public NotificationNotFoundException(String publicId, String message) {
        super(message);
        this.publicId = publicId;
    }

    /**
     * Gets the public ID that was not found.
     *
     * @return the notification's public ID
     */
    public String getPublicId() {
        return publicId;
    }
}