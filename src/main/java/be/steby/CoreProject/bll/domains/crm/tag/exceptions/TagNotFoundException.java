package be.steby.CoreProject.bll.domains.crm.tag.exceptions;

/**
 * Exception thrown when a {@code Tag} entity cannot be found by its public identifier.
 */
public class TagNotFoundException extends RuntimeException {

    /**
     * Constructs a new exception for the given tag public identifier.
     *
     * @param publicId the public UUID of the tag that was not found
     */
    public TagNotFoundException(String publicId) {
        super("Tag not found: " + publicId);
    }
}
