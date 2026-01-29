package be.steby.CoreProject.bll.domains.sport.exceptions;

/**
 * Exception thrown when a sport track is not found.
 *
 * @see SportDomainException
 */
public class SportTrackNotFoundException extends SportDomainException {

    public SportTrackNotFoundException(String publicId) {
        super("Sport track not found: " + publicId, 404);
    }

    public SportTrackNotFoundException(String message, Throwable cause) {
        super(message, 404, cause);
    }
}