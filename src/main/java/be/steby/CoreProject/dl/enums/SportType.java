package be.steby.CoreProject.dl.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration of supported sport types.
 *
 * <p>Used to categorize sport tracks and potentially
 * apply different calculation rules (e.g., calories, pace vs speed).
 */
@Getter
@RequiredArgsConstructor
public enum SportType {

    /**
     * Mountain biking (VTT).
     */
    MTB("Mountain Bike", "mtb", true),

    /**
     * Road cycling.
     */
    CYCLING("Cycling", "cycling", true),

    /**
     * Running.
     */
    RUNNING("Running", "running", false),

    /**
     * Trail running.
     */
    TRAIL_RUNNING("Trail Running", "trail", false),

    /**
     * Hiking / Walking.
     */
    HIKING("Hiking", "hiking", false),

    /**
     * Walking.
     */
    WALKING("Walking", "walking", false),

    /**
     * Other / Unknown activity type.
     */
    OTHER("Other", "other", false);

    /**
     * Human-readable display name.
     */
    private final String displayName;

    /**
     * Short code for URLs and APIs.
     */
    private final String code;

    /**
     * Whether this is a cycling-based activity.
     * Used for speed display (km/h) vs pace display (min/km).
     */
    private final boolean cycling;

    /**
     * Finds a SportType by its code.
     *
     * @param code the code to search for
     * @return the matching SportType or OTHER if not found
     */
    public static SportType fromCode(String code) {
        if (code == null) return OTHER;
        for (SportType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return OTHER;
    }
}