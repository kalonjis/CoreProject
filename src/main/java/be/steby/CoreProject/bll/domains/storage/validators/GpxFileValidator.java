package be.steby.CoreProject.bll.domains.storage.validators;

import be.steby.CoreProject.bll.domains.storage.config.StorageConfiguration;
import be.steby.CoreProject.bll.domains.storage.config.StorageConfiguration.CategoryConfig;
import be.steby.CoreProject.dl.enums.FileCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validator for GPX (GPS Exchange Format) files.
 *
 * <p>Uses StAX (Streaming API for XML) for memory-efficient validation.
 * This event-driven approach processes the file as a stream, using constant
 * memory regardless of file size (O(1) instead of O(n)).
 *
 * <p>Validation checks:
 * <ul>
 *   <li>Well-formed XML structure</li>
 *   <li>Valid GPX root element</li>
 *   <li>At least one track ({@code <trk>}) with track points ({@code <trkpt>})</li>
 *   <li>Valid latitude/longitude attributes on track points</li>
 *   <li>Track point count within configured limits</li>
 * </ul>
 *
 * <p>This validator performs structural validation only. Full parsing
 * (distance calculation, elevation profile, etc.) is handled by the
 * sport/activity domain services.
 *
 * @see FileValidator
 * @see <a href="https://www.topografix.com/gpx.asp">GPX Specification</a>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GpxFileValidator implements FileValidator {

    private final StorageConfiguration storageConfig;

    private static final Set<FileCategory> SUPPORTED_CATEGORIES = Set.of(
            FileCategory.GPX_TRACK
    );

    private static final String GPX_ROOT_ELEMENT = "gpx";
    private static final String TRACK_ELEMENT = "trk";
    private static final String TRACK_POINT_ELEMENT = "trkpt";
    private static final String WAYPOINT_ELEMENT = "wpt";
    private static final String LAT_ATTRIBUTE = "lat";
    private static final String LON_ATTRIBUTE = "lon";

    private static final double LAT_MIN = -90.0;
    private static final double LAT_MAX = 90.0;
    private static final double LON_MIN = -180.0;
    private static final double LON_MAX = 180.0;

    private static final int DEFAULT_MAX_TRACK_POINTS = 100_000;

    @Override
    public boolean supports(FileCategory category) {
        return SUPPORTED_CATEGORIES.contains(category);
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public List<String> validate(MultipartFile file, FileCategory category) {
        List<String> errors = new ArrayList<>();

        CategoryConfig config = storageConfig.getCategoryConfig("gpx-track");

        // 1. Validate file size
        long maxSize = config != null ? config.getMaxFileSize() : storageConfig.getMaxFileSize();
        if (file.getSize() > maxSize) {
            errors.add(String.format(
                    "GPX file too large. Maximum: %s, Actual: %s",
                    formatBytes(maxSize),
                    formatBytes(file.getSize())
            ));
            return errors; // Fail fast - don't parse oversized files
        }

        // 2. Validate content type
        String contentType = file.getContentType();
        if (config != null && contentType != null && !config.isTypeAllowed(contentType)) {
            errors.add("File type '" + contentType + "' is not allowed for GPX files");
            return errors;
        }

        // 3. Validate file extension
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".gpx")) {
            errors.add("GPX files must have .gpx extension");
            return errors;
        }

        // 4. Stream-validate GPX structure with StAX
        int maxTrackPoints = config != null && config.getMaxTrackPoints() != null
                ? config.getMaxTrackPoints()
                : DEFAULT_MAX_TRACK_POINTS;

        GpxValidationResult result = validateGpxStructure(file, maxTrackPoints);
        errors.addAll(result.errors());

        if (result.errors().isEmpty()) {
            log.debug("GPX validation passed: {} track points, {} waypoints",
                    result.trackPointCount(), result.waypointCount());
        }

        return errors;
    }

    // =========================================================================
    // StAX Streaming Validation
    // =========================================================================

    /**
     * Validates GPX structure using StAX streaming parser.
     *
     * <p>Memory-efficient: processes file as event stream without
     * loading entire DOM into memory.
     *
     * @param file           the GPX file to validate
     * @param maxTrackPoints maximum allowed track points
     * @return validation result with errors and metadata
     */
    private GpxValidationResult validateGpxStructure(MultipartFile file, int maxTrackPoints) {
        List<String> errors = new ArrayList<>();
        int trackPointCount = 0;
        int waypointCount = 0;
        int trackCount = 0;
        boolean foundGpxRoot = false;

        // Configure secure XMLInputFactory
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        try (InputStream inputStream = file.getInputStream()) {
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String elementName = reader.getLocalName().toLowerCase();

                    switch (elementName) {
                        case GPX_ROOT_ELEMENT -> foundGpxRoot = true;

                        case TRACK_ELEMENT -> trackCount++;

                        case TRACK_POINT_ELEMENT -> {
                            trackPointCount++;

                            // Check track point limit (fail fast)
                            if (trackPointCount > maxTrackPoints) {
                                errors.add(String.format(
                                        "GPX file exceeds maximum track points. Limit: %d",
                                        maxTrackPoints
                                ));
                                return new GpxValidationResult(errors, trackPointCount, waypointCount);
                            }

                            // Validate coordinates
                            List<String> coordErrors = validateCoordinates(reader, trackPointCount);
                            errors.addAll(coordErrors);

                            // Stop after first few coordinate errors to avoid spam
                            if (errors.size() > 5) {
                                errors.add("Too many validation errors, stopping validation");
                                return new GpxValidationResult(errors, trackPointCount, waypointCount);
                            }
                        }

                        case WAYPOINT_ELEMENT -> {
                            waypointCount++;
                            // Validate waypoint coordinates too
                            List<String> coordErrors = validateCoordinates(reader, -1);
                            errors.addAll(coordErrors);
                        }
                    }
                }
            }

            reader.close();

        } catch (XMLStreamException e) {
            log.warn("GPX XML parsing error: {}", e.getMessage());
            errors.add("Invalid XML structure: " + sanitizeErrorMessage(e.getMessage()));
            return new GpxValidationResult(errors, trackPointCount, waypointCount);
        } catch (Exception e) {
            log.error("Unexpected error validating GPX file", e);
            errors.add("Failed to process GPX file");
            return new GpxValidationResult(errors, trackPointCount, waypointCount);
        }

        // Post-parsing validations
        if (!foundGpxRoot) {
            errors.add("Invalid GPX file: missing <gpx> root element");
        }

        if (trackCount == 0) {
            errors.add("GPX file contains no tracks. At least one <trk> element is required");
        }

        if (trackPointCount == 0) {
            errors.add("GPX file contains no track points. At least one <trkpt> element is required");
        }

        return new GpxValidationResult(errors, trackPointCount, waypointCount);
    }

    /**
     * Validates lat/lon attributes on a track point or waypoint.
     *
     * @param reader          the XML stream reader positioned at the element
     * @param trackPointIndex the track point index (for error messages), -1 for waypoints
     * @return list of validation errors (empty if valid)
     */
    private List<String> validateCoordinates(XMLStreamReader reader, int trackPointIndex) {
        List<String> errors = new ArrayList<>();

        String latStr = reader.getAttributeValue(null, LAT_ATTRIBUTE);
        String lonStr = reader.getAttributeValue(null, LON_ATTRIBUTE);

        String context = trackPointIndex > 0
                ? "Track point #" + trackPointIndex
                : "Waypoint";

        // Check lat attribute
        if (latStr == null || latStr.isBlank()) {
            errors.add(context + ": missing latitude attribute");
        } else {
            try {
                double lat = Double.parseDouble(latStr);
                if (lat < LAT_MIN || lat > LAT_MAX) {
                    errors.add(String.format(
                            "%s: latitude %s out of range [%.1f, %.1f]",
                            context, latStr, LAT_MIN, LAT_MAX
                    ));
                }
            } catch (NumberFormatException e) {
                errors.add(context + ": invalid latitude value '" + latStr + "'");
            }
        }

        // Check lon attribute
        if (lonStr == null || lonStr.isBlank()) {
            errors.add(context + ": missing longitude attribute");
        } else {
            try {
                double lon = Double.parseDouble(lonStr);
                if (lon < LON_MIN || lon > LON_MAX) {
                    errors.add(String.format(
                            "%s: longitude %s out of range [%.1f, %.1f]",
                            context, lonStr, LON_MIN, LON_MAX
                    ));
                }
            } catch (NumberFormatException e) {
                errors.add(context + ": invalid longitude value '" + lonStr + "'");
            }
        }

        return errors;
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Sanitizes XML parser error messages for user display.
     * Removes internal details that could expose implementation.
     */
    private String sanitizeErrorMessage(String message) {
        if (message == null) {
            return "unknown error";
        }
        // Truncate long messages and remove line/column details if too verbose
        if (message.length() > 100) {
            return message.substring(0, 100) + "...";
        }
        return message;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // =========================================================================
    // Result Record
    // =========================================================================

    /**
     * Result of GPX structure validation.
     *
     * @param errors          list of validation errors (empty if valid)
     * @param trackPointCount number of track points found
     * @param waypointCount   number of waypoints found
     */
    private record GpxValidationResult(
            List<String> errors,
            int trackPointCount,
            int waypointCount
    ) {}
}