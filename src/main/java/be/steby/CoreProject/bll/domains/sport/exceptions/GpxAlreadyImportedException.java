package be.steby.CoreProject.bll.domains.sport.exceptions;

import lombok.Getter;

/**
 * Exception thrown when attempting to import a GPX file that was already imported.
 *
 * @see SportDomainException
 */
@Getter
public class GpxAlreadyImportedException extends SportDomainException {

    private final String gpxFilePublicId;

    public GpxAlreadyImportedException(String gpxFilePublicId) {
        super("GPX file already imported: " + gpxFilePublicId, 409);
        this.gpxFilePublicId = gpxFilePublicId;
    }
}