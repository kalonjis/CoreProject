package be.steby.CoreProject.pl.domains.gdpr.controllers;

import be.steby.CoreProject.bll.domains.gdpr.models.GdprDownloadResult;
import be.steby.CoreProject.bll.domains.gdpr.services.data_export.GdprExportService;
import be.steby.CoreProject.dl.entities.GdprExportRequest;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.domains.gdpr.models.responses.GdprExportStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

/**
 * REST Controller for GDPR data export operations.
 *
 * <p>Endpoints:
 * <pre>
 * POST /api/privacy/export/request    — authenticated — initiates an export request
 * GET  /api/privacy/export/confirm    — public (email link) — confirms and triggers generation
 * GET  /api/privacy/export/download   — public (email link) — serves or redirects to archive
 * GET  /api/privacy/export/status     — authenticated — polls current request status
 * </pre>
 *
 * <p>All business logic is delegated to {@link GdprExportService}.
 * This controller only handles HTTP concerns (status codes, headers, response format).
 */
@RestController
@RequestMapping("/api/privacy/export")
@RequiredArgsConstructor
@Slf4j
public class GdprExportController {

    private final GdprExportService gdprExportService;

    // =========================================================================
    // POST /request — authenticated
    // =========================================================================

    /**
     * Initiates a GDPR data export request.
     *
     * <p>On success: creates a PENDING request and sends a confirmation email.
     * Returns 202 Accepted — the archive is not yet ready.
     *
     * @param user the authenticated user
     * @return 202 with a confirmation message
     */
    @PostMapping("/request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> request(@AuthenticationPrincipal User user) {
        log.info("GDPR export requested by user: {}", user.getUsername());

        gdprExportService.request(user);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    // =========================================================================
    // GET /confirm?token= — public (email link)
    // =========================================================================

    /**
     * Confirms the export request via the token received by email.
     * Triggers asynchronous archive generation.
     *
     * <p>Returns 202 Accepted — generation has started but is not yet complete.
     * The user will receive a second email when the archive is ready.
     *
     * @param token the single-use token from the confirmation email
     * @return 202 with a confirmation message
     */
    @GetMapping("/confirm")
    public ResponseEntity<Void> confirm(@RequestParam String token) {
        log.info("GDPR export confirmation received for token: {}", token);

        gdprExportService.confirm(token);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    // =========================================================================
    // GET /download?token= — public (email link)
    // =========================================================================

    /**
     * Serves the GDPR archive or redirects to a pre-signed URL.
     *
     * <p>Behavior depends on the active storage provider:
     * <ul>
     *   <li>Local: streams the ZIP file directly (200 + Content-Disposition)</li>
     *   <li>R2: redirects to a pre-signed URL (302)</li>
     * </ul>
     *
     * <p>After a successful response: the archive is deleted from storage
     * and the request is marked as DOWNLOADED.
     *
     * @param token the single-use download token from the email
     * @return 200 with ZIP content (local) or 302 redirect (R2)
     */
    @GetMapping("/download")
    public ResponseEntity<?> download(@RequestParam String token) {
        log.info("GDPR archive download requested for token: {}", token);

        GdprDownloadResult result = gdprExportService.download(token);

        if (result.isRedirect()) {
            // R2 mode — redirect to pre-signed URL
            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .location(URI.create(result.redirectUrl()))
                    .build();
        }

        // Local mode — stream the ZIP directly
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.filename() + "\""
                )
                .body(result.fileBytes());
    }

    // =========================================================================
    // GET /status — authenticated
    // =========================================================================

    /**
     * Returns the current GDPR export request status for the authenticated user.
     * Used by the frontend to drive UI state (polling or on page load).
     *
     * @param user the authenticated user
     * @return 200 with status DTO (status is null if no request has ever been made)
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GdprExportStatusResponse> status(@AuthenticationPrincipal User user) {
        Optional<GdprExportRequest> exportRequest = gdprExportService.getStatus(user);

        GdprExportStatusResponse response = exportRequest
                .map(GdprExportStatusResponse::from)
                .orElse(GdprExportStatusResponse.none());

        return ResponseEntity.ok(response);
    }
}