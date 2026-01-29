package be.steby.CoreProject.pl.domains.sport.controllers;

import be.steby.CoreProject.bll.domains.sport.services.SportTrackService;
import be.steby.CoreProject.dl.entities.SportTrack;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.SportType;
import be.steby.CoreProject.pl.domains.sport.models.requests.ImportGpxRequest;
import be.steby.CoreProject.pl.domains.sport.models.requests.UpdateSportTrackRequest;
import be.steby.CoreProject.pl.domains.sport.models.responses.OwnerStatsResponse;
import be.steby.CoreProject.pl.domains.sport.models.responses.SportTrackResponse;
import be.steby.CoreProject.pl.domains.sport.models.responses.SportTrackSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for sport track operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Import GPX files as sport tracks</li>
 *   <li>List, view, update, delete sport tracks</li>
 *   <li>Get aggregated statistics</li>
 * </ul>
 *
 * <p>All endpoints require authentication. Users can only access their own tracks.
 *
 * @see SportTrackService
 */
@RestController
@RequestMapping("/api/sport-tracks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sport Tracks", description = "Sport track management and GPX import")
public class SportTrackController {

    private final SportTrackService sportTrackService;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // Import
    // =========================================================================

    /**
     * Imports a GPX file and creates a new sport track.
     *
     * @param request the import request with GPX file ID and sport type
     * @param user    the authenticated user
     * @return the created sport track
     */
    @PostMapping("/import")
    @Operation(summary = "Import GPX file", description = "Creates a sport track from an uploaded GPX file")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sport track created"),
            @ApiResponse(responseCode = "400", description = "Invalid GPX file"),
            @ApiResponse(responseCode = "404", description = "GPX file not found"),
            @ApiResponse(responseCode = "409", description = "GPX file already imported")
    })
    public ResponseEntity<SportTrackResponse> importGpx(
            @Valid @RequestBody ImportGpxRequest request,
            @AuthenticationPrincipal User user
    ) {
        log.info("Importing GPX file {} for user {}", request.gpxFilePublicId(), user.getUsername());

        SportTrack track = sportTrackService.importFromGpx(
                request.gpxFilePublicId(),
                user.getPublicId(),
                request.sportTypeOrDefault()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(SportTrackResponse.fromEntity(track, objectMapper));
    }

    // =========================================================================
    // Read Operations
    // =========================================================================

    /**
     * Lists all sport tracks for the authenticated user.
     *
     * @param sportType optional filter by sport type
     * @param pageable  pagination parameters
     * @param user      the authenticated user
     * @return page of sport track summaries
     */
    @GetMapping
    @Operation(summary = "List sport tracks", description = "Returns paginated list of user's sport tracks")
    public ResponseEntity<Page<SportTrackSummaryResponse>> list(
            @Parameter(description = "Filter by sport type")
            @RequestParam(required = false) SportType sportType,

            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC)
            Pageable pageable,

            @AuthenticationPrincipal User user
    ) {
        Page<SportTrack> tracks;

        if (sportType != null) {
            tracks = sportTrackService.findByOwnerAndType(user.getPublicId(), sportType, pageable);
        } else {
            tracks = sportTrackService.findByOwner(user.getPublicId(), pageable);
        }

        return ResponseEntity.ok(tracks.map(SportTrackSummaryResponse::fromEntity));
    }

    /**
     * Gets a sport track by its public ID.
     *
     * @param publicId the track's public ID
     * @param user     the authenticated user
     * @return the sport track details
     */
    @GetMapping("/{publicId}")
    @Operation(summary = "Get sport track", description = "Returns full sport track details including track data")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sport track found"),
            @ApiResponse(responseCode = "404", description = "Sport track not found")
    })
    public ResponseEntity<SportTrackResponse> getByPublicId(
            @PathVariable String publicId,
            @AuthenticationPrincipal User user
    ) {
        SportTrack track = sportTrackService.getByPublicIdAndOwner(publicId, user.getPublicId());
        return ResponseEntity.ok(SportTrackResponse.fromEntity(track, objectMapper));
    }

    /**
     * Gets aggregated statistics for the authenticated user.
     *
     * @param user the authenticated user
     * @return statistics summary
     */
    @GetMapping("/stats")
    @Operation(summary = "Get statistics", description = "Returns aggregated statistics for all user's tracks")
    public ResponseEntity<OwnerStatsResponse> getStats(@AuthenticationPrincipal User user) {
        var stats = sportTrackService.getOwnerStats(user.getPublicId());
        return ResponseEntity.ok(OwnerStatsResponse.fromStats(stats));
    }

    // =========================================================================
    // Update Operations
    // =========================================================================

    /**
     * Updates a sport track's metadata.
     *
     * @param publicId the track's public ID
     * @param request  the update request
     * @param user     the authenticated user
     * @return the updated sport track
     */
    @PatchMapping("/{publicId}")
    @Operation(summary = "Update sport track", description = "Updates name and/or sport type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sport track updated"),
            @ApiResponse(responseCode = "404", description = "Sport track not found")
    })
    public ResponseEntity<SportTrackResponse> update(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateSportTrackRequest request,
            @AuthenticationPrincipal User user
    ) {
        log.info("Updating sport track {} for user {}", publicId, user.getUsername());

        SportTrack track = sportTrackService.update(
                publicId,
                user.getPublicId(),
                request.name(),
                request.sportType()
        );

        return ResponseEntity.ok(SportTrackResponse.fromEntity(track, objectMapper));
    }

    // =========================================================================
    // Delete Operations
    // =========================================================================

    /**
     * Deletes a sport track.
     *
     * @param publicId the track's public ID
     * @param user     the authenticated user
     * @return no content
     */
    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete sport track", description = "Permanently deletes a sport track")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sport track deleted"),
            @ApiResponse(responseCode = "404", description = "Sport track not found")
    })
    public ResponseEntity<Void> delete(
            @PathVariable String publicId,
            @AuthenticationPrincipal User user
    ) {
        log.info("Deleting sport track {} for user {}", publicId, user.getUsername());

        sportTrackService.delete(publicId, user.getPublicId());

        return ResponseEntity.noContent().build();
    }
}