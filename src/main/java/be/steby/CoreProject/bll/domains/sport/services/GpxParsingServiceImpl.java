package be.steby.CoreProject.bll.domains.sport.services;

import be.steby.CoreProject.bll.domains.sport.exceptions.GpxParsingException;
import be.steby.CoreProject.bll.domains.sport.models.GpxParseResult;
import be.steby.CoreProject.bll.domains.sport.models.TrackPoint;
import be.steby.CoreProject.bll.domains.sport.services.GpxParsingService;
import be.steby.CoreProject.bll.domains.storage.services.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of GPX parsing service using StAX.
 *
 * <p>Uses streaming XML parsing for memory efficiency.
 * Calculates all metrics during a single pass when possible.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GpxParsingServiceImpl implements GpxParsingService {

    private final FileStorageService fileStorageService;

    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private static final double DEFAULT_ELEVATION_THRESHOLD = 3.0; // meters
    private static final int DEFAULT_SIMPLIFIED_MAX_POINTS = 500;

    // =========================================================================
    // Public API
    // =========================================================================

    @Override
    public GpxParseResult parse(InputStream inputStream, String filename) {
        log.debug("Parsing GPX file: {}", filename);

        try {
            // 1. Extract all track points
            GpxRawData rawData = extractRawData(inputStream);

            if (rawData.trackPoints.isEmpty()) {
                throw new GpxParsingException("GPX file contains no track points");
            }

            // 2. Calculate all metrics
            return buildParseResult(rawData, filename);

        } catch (XMLStreamException e) {
            log.error("XML parsing error for file {}: {}", filename, e.getMessage());
            throw new GpxParsingException("Invalid GPX file format: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error parsing GPX file {}: {}", filename, e.getMessage(), e);
            throw new GpxParsingException("Failed to parse GPX file: " + e.getMessage(), e);
        }
    }

    @Override
    public GpxParseResult parseFromStorage(String gpxFilePublicId) {
        log.debug("Parsing GPX from storage: {}", gpxFilePublicId);

        Resource resource = fileStorageService.loadAsResource(gpxFilePublicId);

        try (InputStream is = resource.getInputStream()) {
            String filename = resource.getFilename() != null ? resource.getFilename() : "unknown.gpx";
            return parse(is, filename);
        } catch (IOException e) {
            throw new GpxParsingException("Failed to read GPX file from storage", e);
        }
    }

    @Override
    public List<TrackPoint> simplifyTrack(List<TrackPoint> points, int maxPoints) {
        if (points.size() <= maxPoints) {
            return new ArrayList<>(points);
        }

        // Douglas-Peucker simplification
        return douglasPeucker(points, calculateEpsilon(points, maxPoints));
    }

    @Override
    public double calculateDistance(List<TrackPoint> points) {
        if (points.size() < 2) return 0.0;

        double totalDistance = 0.0;
        for (int i = 1; i < points.size(); i++) {
            totalDistance += haversineDistance(points.get(i - 1), points.get(i));
        }
        return totalDistance;
    }

    @Override
    public double calculateElevationGain(List<TrackPoint> points, double threshold) {
        return calculateElevationChange(points, threshold, true);
    }

    @Override
    public double calculateElevationLoss(List<TrackPoint> points, double threshold) {
        return calculateElevationChange(points, threshold, false);
    }

    // =========================================================================
    // XML Parsing (StAX)
    // =========================================================================

    private GpxRawData extractRawData(InputStream inputStream) throws XMLStreamException {
        List<TrackPoint> trackPoints = new ArrayList<>();
        String trackName = null;

        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        XMLStreamReader reader = factory.createXMLStreamReader(inputStream);

        Double currentLat = null;
        Double currentLon = null;
        Double currentEle = null;
        Instant currentTime = null;
        StringBuilder textContent = new StringBuilder();
        boolean inTrackPoint = false;
        boolean inTrackName = false;

        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    String name = reader.getLocalName().toLowerCase();

                    if ("trkpt".equals(name)) {
                        inTrackPoint = true;
                        currentLat = parseDouble(reader.getAttributeValue(null, "lat"));
                        currentLon = parseDouble(reader.getAttributeValue(null, "lon"));
                        currentEle = null;
                        currentTime = null;
                    } else if ("name".equals(name) && trackName == null) {
                        inTrackName = true;
                        textContent.setLength(0);
                    } else if (inTrackPoint && ("ele".equals(name) || "time".equals(name))) {
                        textContent.setLength(0);
                    }
                }

                case XMLStreamConstants.CHARACTERS -> {
                    if (inTrackPoint || inTrackName) {
                        textContent.append(reader.getText());
                    }
                }

                case XMLStreamConstants.END_ELEMENT -> {
                    String name = reader.getLocalName().toLowerCase();

                    if ("trkpt".equals(name)) {
                        if (currentLat != null && currentLon != null) {
                            trackPoints.add(new TrackPoint(currentLat, currentLon, currentEle, currentTime));
                        }
                        inTrackPoint = false;
                    } else if ("name".equals(name) && inTrackName) {
                        trackName = textContent.toString().trim();
                        inTrackName = false;
                    } else if (inTrackPoint) {
                        if ("ele".equals(name)) {
                            currentEle = parseDouble(textContent.toString().trim());
                        } else if ("time".equals(name)) {
                            currentTime = parseTimestamp(textContent.toString().trim());
                        }
                    }
                }
            }
        }

        reader.close();

        return new GpxRawData(trackPoints, trackName);
    }

    // =========================================================================
    // Metrics Calculation
    // =========================================================================

    private GpxParseResult buildParseResult(GpxRawData rawData, String filename) {
        List<TrackPoint> points = rawData.trackPoints;

        // Time metrics
        Instant startedAt = points.stream()
                .filter(TrackPoint::hasTimestamp)
                .map(TrackPoint::timestamp)
                .findFirst()
                .orElse(Instant.now());

        Instant endedAt = points.stream()
                .filter(TrackPoint::hasTimestamp)
                .map(TrackPoint::timestamp)
                .reduce((a, b) -> b)
                .orElse(startedAt);

        long durationSeconds = Duration.between(startedAt, endedAt).getSeconds();

        // Distance
        double distanceMeters = calculateDistance(points);

        // Speed
        double avgSpeedMps = durationSeconds > 0 ? distanceMeters / durationSeconds : 0.0;
        double maxSpeedMps = calculateMaxSpeed(points);

        // Elevation
        List<TrackPoint> pointsWithElevation = points.stream()
                .filter(TrackPoint::hasElevation)
                .toList();

        Double elevationMin = pointsWithElevation.stream()
                .mapToDouble(TrackPoint::elevation)
                .min().stream().boxed().findFirst().orElse(null);

        Double elevationMax = pointsWithElevation.stream()
                .mapToDouble(TrackPoint::elevation)
                .max().stream().boxed().findFirst().orElse(null);

        Double elevationGain = !pointsWithElevation.isEmpty()
                ? calculateElevationGain(pointsWithElevation, DEFAULT_ELEVATION_THRESHOLD)
                : null;

        Double elevationLoss = !pointsWithElevation.isEmpty()
                ? calculateElevationLoss(pointsWithElevation, DEFAULT_ELEVATION_THRESHOLD)
                : null;

        // Bounding box
        double minLat = points.stream().mapToDouble(TrackPoint::latitude).min().orElse(0);
        double maxLat = points.stream().mapToDouble(TrackPoint::latitude).max().orElse(0);
        double minLon = points.stream().mapToDouble(TrackPoint::longitude).min().orElse(0);
        double maxLon = points.stream().mapToDouble(TrackPoint::longitude).max().orElse(0);

        // Simplify track
        List<TrackPoint> simplified = simplifyTrack(points, DEFAULT_SIMPLIFIED_MAX_POINTS);

        // Name
        String name = rawData.trackName != null ? rawData.trackName : extractNameFromFilename(filename);

        return GpxParseResult.builder()
                .name(name)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .trackPoints(points)
                .simplifiedTrack(simplified)
                .totalTrackPoints(points.size())
                .distanceMeters(distanceMeters)
                .durationSeconds(durationSeconds)
                .avgSpeedMps(avgSpeedMps)
                .maxSpeedMps(maxSpeedMps)
                .elevationMin(elevationMin)
                .elevationMax(elevationMax)
                .elevationGain(elevationGain)
                .elevationLoss(elevationLoss)
                .boundsMinLat(minLat)
                .boundsMaxLat(maxLat)
                .boundsMinLon(minLon)
                .boundsMaxLon(maxLon)
                .build();
    }

    private double calculateMaxSpeed(List<TrackPoint> points) {
        if (points.size() < 2) return 0.0;

        double maxSpeed = 0.0;
        for (int i = 1; i < points.size(); i++) {
            TrackPoint p1 = points.get(i - 1);
            TrackPoint p2 = points.get(i);

            if (p1.hasTimestamp() && p2.hasTimestamp()) {
                double distance = haversineDistance(p1, p2);
                long seconds = Duration.between(p1.timestamp(), p2.timestamp()).getSeconds();

                if (seconds > 0) {
                    double speed = distance / seconds;
                    // Filter unrealistic speeds (GPS glitches) - max 150 km/h for cycling
                    if (speed < 42 && speed > maxSpeed) {
                        maxSpeed = speed;
                    }
                }
            }
        }
        return maxSpeed;
    }

    private double calculateElevationChange(List<TrackPoint> points, double threshold, boolean gain) {
        if (points.size() < 2) return 0.0;

        double totalChange = 0.0;
        double accumulated = 0.0;
        Double lastElevation = null;

        for (TrackPoint point : points) {
            if (!point.hasElevation()) continue;

            if (lastElevation != null) {
                double diff = point.elevation() - lastElevation;

                // Accumulate changes in same direction
                if ((gain && diff > 0) || (!gain && diff < 0)) {
                    accumulated += Math.abs(diff);
                } else if (accumulated >= threshold) {
                    // Direction changed and accumulated exceeds threshold
                    totalChange += accumulated;
                    accumulated = Math.abs(diff);
                } else {
                    // Direction changed but below threshold, reset
                    accumulated = Math.abs(diff);
                }
            }
            lastElevation = point.elevation();
        }

        // Don't forget last accumulated segment
        if (accumulated >= threshold) {
            totalChange += accumulated;
        }

        return totalChange;
    }

    // =========================================================================
    // Haversine Distance
    // =========================================================================

    private double haversineDistance(TrackPoint p1, TrackPoint p2) {
        double lat1Rad = Math.toRadians(p1.latitude());
        double lat2Rad = Math.toRadians(p2.latitude());
        double deltaLat = Math.toRadians(p2.latitude() - p1.latitude());
        double deltaLon = Math.toRadians(p2.longitude() - p1.longitude());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    // =========================================================================
    // Douglas-Peucker Simplification
    // =========================================================================

    private List<TrackPoint> douglasPeucker(List<TrackPoint> points, double epsilon) {
        if (points.size() < 3) {
            return new ArrayList<>(points);
        }

        // Find point with maximum distance from line between first and last
        double maxDistance = 0;
        int maxIndex = 0;

        TrackPoint first = points.get(0);
        TrackPoint last = points.get(points.size() - 1);

        for (int i = 1; i < points.size() - 1; i++) {
            double distance = perpendicularDistance(points.get(i), first, last);
            if (distance > maxDistance) {
                maxDistance = distance;
                maxIndex = i;
            }
        }

        // If max distance > epsilon, recursively simplify
        if (maxDistance > epsilon) {
            List<TrackPoint> left = douglasPeucker(points.subList(0, maxIndex + 1), epsilon);
            List<TrackPoint> right = douglasPeucker(points.subList(maxIndex, points.size()), epsilon);

            // Combine results (remove duplicate point at junction)
            List<TrackPoint> result = new ArrayList<>(left.subList(0, left.size() - 1));
            result.addAll(right);
            return result;
        } else {
            // All points between first and last can be removed
            List<TrackPoint> result = new ArrayList<>();
            result.add(first);
            result.add(last);
            return result;
        }
    }

    private double perpendicularDistance(TrackPoint point, TrackPoint lineStart, TrackPoint lineEnd) {
        double dx = lineEnd.longitude() - lineStart.longitude();
        double dy = lineEnd.latitude() - lineStart.latitude();

        double mag = Math.sqrt(dx * dx + dy * dy);
        if (mag == 0) return haversineDistance(point, lineStart);

        double u = ((point.longitude() - lineStart.longitude()) * dx +
                    (point.latitude() - lineStart.latitude()) * dy) / (mag * mag);

        double closestLon, closestLat;
        if (u < 0) {
            closestLon = lineStart.longitude();
            closestLat = lineStart.latitude();
        } else if (u > 1) {
            closestLon = lineEnd.longitude();
            closestLat = lineEnd.latitude();
        } else {
            closestLon = lineStart.longitude() + u * dx;
            closestLat = lineStart.latitude() + u * dy;
        }

        return haversineDistance(point, new TrackPoint(closestLat, closestLon, null, null));
    }

    private double calculateEpsilon(List<TrackPoint> points, int targetPoints) {
        // Estimate epsilon based on track bounds and target point count
        double minLat = points.stream().mapToDouble(TrackPoint::latitude).min().orElse(0);
        double maxLat = points.stream().mapToDouble(TrackPoint::latitude).max().orElse(0);
        double minLon = points.stream().mapToDouble(TrackPoint::longitude).min().orElse(0);
        double maxLon = points.stream().mapToDouble(TrackPoint::longitude).max().orElse(0);

        double diagonal = Math.sqrt(Math.pow(maxLat - minLat, 2) + Math.pow(maxLon - minLon, 2));
        return diagonal / targetPoints * 0.5;
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Instant parseTimestamp(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException e) {
            // Try with timezone offset format
            try {
                return java.time.OffsetDateTime.parse(value.trim()).toInstant();
            } catch (DateTimeParseException e2) {
                log.trace("Failed to parse timestamp: {}", value);
                return null;
            }
        }
    }

    private String extractNameFromFilename(String filename) {
        if (filename == null) return "Untitled Track";
        String name = filename;
        if (name.toLowerCase().endsWith(".gpx")) {
            name = name.substring(0, name.length() - 4);
        }
        return name.replace("_", " ").replace("-", " ");
    }

    // =========================================================================
    // Inner Classes
    // =========================================================================

    private record GpxRawData(List<TrackPoint> trackPoints, String trackName) {}
}