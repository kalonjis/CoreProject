package be.steby.CoreProject.pl.domains.sport.models.requests;

import be.steby.CoreProject.dl.enums.SportType;
import jakarta.validation.constraints.Size;

/**
 * Request to update a sport track's metadata.
 *
 * @param name      new name (optional)
 * @param sportType new sport type (optional)
 */
public record UpdateSportTrackRequest(
        @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
        String name,

        SportType sportType
) {}