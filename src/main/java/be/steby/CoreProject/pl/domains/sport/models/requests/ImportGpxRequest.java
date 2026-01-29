package be.steby.CoreProject.pl.domains.sport.models.requests;

import be.steby.CoreProject.dl.enums.SportType;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to import a GPX file as a sport track.
 *
 * @param gpxFilePublicId public ID of the uploaded GPX file
 * @param sportType       type of sport activity (optional, defaults to OTHER)
 */
public record ImportGpxRequest(
        @NotBlank(message = "GPX file public ID is required")
        String gpxFilePublicId,

        SportType sportType
) {
    /**
     * Returns sport type or default.
     */
    public SportType sportTypeOrDefault() {
        return sportType != null ? sportType : SportType.OTHER;
    }
}