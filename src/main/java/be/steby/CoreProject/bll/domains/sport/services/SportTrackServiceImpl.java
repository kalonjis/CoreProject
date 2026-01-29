package be.steby.CoreProject.bll.domains.sport.services;

import be.steby.CoreProject.bll.domains.sport.exceptions.GpxAlreadyImportedException;
import be.steby.CoreProject.bll.domains.sport.exceptions.SportTrackNotFoundException;
import be.steby.CoreProject.bll.domains.sport.models.GpxParseResult;
import be.steby.CoreProject.bll.domains.sport.models.TrackPoint;
import be.steby.CoreProject.bll.domains.sport.services.GpxParsingService;
import be.steby.CoreProject.bll.domains.sport.services.SportTrackService;
import be.steby.CoreProject.dal.repositories.SportTrackRepository;
import be.steby.CoreProject.dl.entities.SportTrack;
import be.steby.CoreProject.dl.enums.SportType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of SportTrackService.
 *
 * <p>Orchestrates GPX parsing and persistence of sport tracks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SportTrackServiceImpl implements SportTrackService {

    private final SportTrackRepository sportTrackRepository;
    private final GpxParsingService gpxParsingService;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // Import
    // =========================================================================

    @Override
    @Transactional
    public SportTrack importFromGpx(String gpxFilePublicId, String ownerPublicId, SportType sportType) {
        log.info("Importing GPX file {} for owner {} as {}", gpxFilePublicId, ownerPublicId, sportType);

        // Check if already imported
        if (sportTrackRepository.existsByGpxFilePublicId(gpxFilePublicId)) {
            throw new GpxAlreadyImportedException(gpxFilePublicId);
        }

        // Parse GPX file
        GpxParseResult parseResult = gpxParsingService.parseFromStorage(gpxFilePublicId);

        // Build entity
        SportTrack sportTrack = buildSportTrack(parseResult, gpxFilePublicId, ownerPublicId, sportType);

        // DEBUG - à supprimer après
        log.info("DEBUG simplified_track: length={}, preview={}",
                sportTrack.getSimplifiedTrack().length(),
                sportTrack.getSimplifiedTrack().substring(0, Math.min(100, sportTrack.getSimplifiedTrack().length())));

        // Persist
        SportTrack saved = sportTrackRepository.save(sportTrack);
        log.info("Sport track created: publicId={}, distance={}km, duration={}s",
                saved.getPublicId(), saved.getDistanceKm(), saved.getDurationSeconds());

        return saved;
    }

    @Override
    @Transactional
    public SportTrack importFromGpx(String gpxFilePublicId, String ownerPublicId) {
        return importFromGpx(gpxFilePublicId, ownerPublicId, SportType.OTHER);
    }

    // =========================================================================
    // Read Operations
    // =========================================================================

    @Override
    public Optional<SportTrack> findByPublicId(String publicId) {
        return sportTrackRepository.findByPublicId(publicId);
    }

    @Override
    public Optional<SportTrack> findByPublicIdAndOwner(String publicId, String ownerPublicId) {
        return sportTrackRepository.findByPublicIdAndOwnerPublicId(publicId, ownerPublicId);
    }

    @Override
    public SportTrack getByPublicId(String publicId) {
        return findByPublicId(publicId)
                .orElseThrow(() -> new SportTrackNotFoundException(publicId));
    }

    @Override
    public SportTrack getByPublicIdAndOwner(String publicId, String ownerPublicId) {
        return findByPublicIdAndOwner(publicId, ownerPublicId)
                .orElseThrow(() -> new SportTrackNotFoundException(publicId));
    }

    @Override
    public Page<SportTrack> findByOwner(String ownerPublicId, Pageable pageable) {
        return sportTrackRepository.findByOwnerPublicId(ownerPublicId, pageable);
    }

    @Override
    public Page<SportTrack> findByOwnerAndType(String ownerPublicId, SportType sportType, Pageable pageable) {
        return sportTrackRepository.findByOwnerPublicIdAndSportType(ownerPublicId, sportType, pageable);
    }

    // =========================================================================
    // Update Operations
    // =========================================================================

    @Override
    @Transactional
    public SportTrack update(String publicId, String ownerPublicId, String name, SportType sportType) {
        SportTrack sportTrack = getByPublicIdAndOwner(publicId, ownerPublicId);

        if (name != null && !name.isBlank()) {
            sportTrack.setName(name.trim());
        }

        if (sportType != null) {
            sportTrack.setSportType(sportType);
        }

        return sportTrackRepository.save(sportTrack);
    }

    // =========================================================================
    // Delete Operations
    // =========================================================================

    @Override
    @Transactional
    public void delete(String publicId, String ownerPublicId) {
        SportTrack sportTrack = getByPublicIdAndOwner(publicId, ownerPublicId);
        sportTrackRepository.delete(sportTrack);
        log.info("Sport track deleted: publicId={}", publicId);
    }

    // =========================================================================
    // Statistics
    // =========================================================================

    @Override
    public OwnerStats getOwnerStats(String ownerPublicId) {
        long totalTracks = sportTrackRepository.countByOwnerPublicId(ownerPublicId);

        Double totalDistanceM = sportTrackRepository.sumDistanceByOwner(ownerPublicId);
        Long totalDurationS = sportTrackRepository.sumDurationByOwner(ownerPublicId);
        Double totalElevationGainM = sportTrackRepository.sumElevationGainByOwner(ownerPublicId);

        return new OwnerStats(
                totalTracks,
                totalDistanceM != null ? totalDistanceM / 1000.0 : 0.0,
                totalDurationS != null ? totalDurationS : 0L,
                totalElevationGainM != null ? totalElevationGainM : 0.0
        );
    }

    // =========================================================================
    // Private Methods
    // =========================================================================

    private SportTrack buildSportTrack(
            GpxParseResult result,
            String gpxFilePublicId,
            String ownerPublicId,
            SportType sportType
    ) {
        return SportTrack.builder()
                // Ownership
                .ownerPublicId(ownerPublicId)
                .gpxFilePublicId(gpxFilePublicId)
                // Metadata
                .name(result.getName())
                .sportType(sportType)
                .startedAt(result.getStartedAt())
                .endedAt(result.getEndedAt())
                // Metrics
                .distanceMeters(result.getDistanceMeters())
                .durationSeconds(result.getDurationSeconds())
                .avgSpeedMps(result.getAvgSpeedMps())
                .maxSpeedMps(result.getMaxSpeedMps())
                .elevationMin(result.getElevationMin())
                .elevationMax(result.getElevationMax())
                .elevationGain(result.getElevationGain())
                .elevationLoss(result.getElevationLoss())
                // Track data
                .totalTrackPoints(result.getTotalTrackPoints())
                .simplifiedTrack(serializeTrackPoints(result.getSimplifiedTrack()))
                .elevationProfile(serializeElevationProfile(result))
                // Bounds
                .boundsMinLat(result.getBoundsMinLat())
                .boundsMaxLat(result.getBoundsMaxLat())
                .boundsMinLon(result.getBoundsMinLon())
                .boundsMaxLon(result.getBoundsMaxLon())
                .build();
    }

    /**
     * Serializes track points to JSON array: [[lat, lon, ele], ...]
     */
    private String serializeTrackPoints(List<TrackPoint> points) {
        try {
            double[][] data = points.stream()
                    .map(TrackPoint::toArray)
                    .toArray(double[][]::new);
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize track points", e);
            return "[]";
        }
    }

    /**
     * Serializes elevation profile to JSON array: [[distanceKm, elevationM], ...]
     */
    private String serializeElevationProfile(GpxParseResult result) {
        try {
            List<TrackPoint> points = result.getSimplifiedTrack();
            double[][] profile = new double[points.size()][2];

            double cumulativeDistance = 0.0;
            TrackPoint previous = null;

            for (int i = 0; i < points.size(); i++) {
                TrackPoint current = points.get(i);

                if (previous != null) {
                    cumulativeDistance += gpxParsingService.calculateDistance(List.of(previous, current));
                }

                profile[i][0] = cumulativeDistance / 1000.0; // km
                profile[i][1] = current.elevation() != null ? current.elevation() : 0.0;

                previous = current;
            }

            return objectMapper.writeValueAsString(profile);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize elevation profile", e);
            return "[]";
        }
    }
}