package be.steby.CoreProject.bll.domains.gdpr.exceptions;

import be.steby.CoreProject.bll.domains.storage.exceptions.StorageDomainException;

/**
 * Thrown when a GDPR archive storage operation fails.
 */
public class GdprStorageException extends GdprDomainException {

    public GdprStorageException(String message) {
        super(message);
    }

    public GdprStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}