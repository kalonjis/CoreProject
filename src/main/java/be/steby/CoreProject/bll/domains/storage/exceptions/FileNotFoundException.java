package be.steby.CoreProject.bll.domains.storage.exceptions;

/**
 * Exception thrown when a requested file is not found.
 *
 * <p>Used when:
 * <ul>
 *   <li>File metadata not found in database</li>
 *   <li>Physical file missing from storage</li>
 *   <li>File exists but is not accessible (deleted/archived)</li>
 * </ul>
 *
 * <p>HTTP Status: 404 (Not Found).</p>
 *
 * @see StorageDomainException
 */
public class FileNotFoundException extends StorageDomainException {

    private static final int STATUS = 404;

    /**
     * Creates exception for a file not found by publicId.
     *
     * @param publicId the file's public ID
     */
    public FileNotFoundException(String publicId) {
        super("File not found: " + publicId, STATUS);
    }

    /**
     * Creates exception with custom message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public FileNotFoundException(String message, Throwable cause) {
        super(message, STATUS, cause);
    }
}