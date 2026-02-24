package be.steby.CoreProject.bll.domains.gdpr.services.data_export;

import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.gdpr.events.GdprExportReadyEvent;
import be.steby.CoreProject.bll.domains.gdpr.events.GdprExportRequestedEvent;
import be.steby.CoreProject.bll.domains.gdpr.exceptions.GdprExportException;
import be.steby.CoreProject.bll.domains.gdpr.models.GdprDownloadResult;
import be.steby.CoreProject.bll.domains.gdpr.models.GdprUserDataSnapshot;
import be.steby.CoreProject.bll.domains.gdpr.services.data_collector.GdprDataCollectorService;
import be.steby.CoreProject.bll.domains.gdpr.services.storage.GdprStorageService;
import be.steby.CoreProject.dal.repositories.GdprExportRequestRepository;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.GdprExportRequest;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.GdprExportStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Implementation of {@link GdprExportService}.
 *
 * <p>Orchestrates the full export lifecycle: request → confirm → generate → download.
 * Archive generation runs on the {@code gdprExecutor} thread pool (defined in AsyncConfig).
 *
 * <p>The ZIP archive contains one JSON file per data domain:
 * <pre>
 * gdpr-export-{username}-{date}.zip
 *   ├── profile.json
 *   ├── addresses.json
 *   ├── devices.json
 *   ├── activity.json
 *   ├── notifications.json
 *   ├── calendar.json
 *   └── sport.json
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GdprExportServiceImpl implements GdprExportService {

    private final GdprExportRequestRepository exportRequestRepository;
    private final GdprDataCollectorService dataCollectorService;
    private final GdprStorageService storageService;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher    eventPublisher;
    private final ObjectMapper                 objectMapper;

    @Value("${app.gdpr.export.archive-ttl-hours:72}")
    private int archiveTtlHours;

    @Value("${app.gdpr.export.cooldown-days:7}")
    private int cooldownDays;

    @Value("${url.front_server}")
    private String frontUrl;

    // =========================================================================
    // 1. Request
    // =========================================================================

    @Override
    @Transactional
    public GdprExportRequest request(User user) {
        log.info("GDPR export request initiated by user: {}", user.getUsername());

        // Guard: no active request already in progress
        List<GdprExportStatus> activeStatuses = List.of(
            GdprExportStatus.PENDING,
            GdprExportStatus.PROCESSING,
            GdprExportStatus.READY
        );
        exportRequestRepository.findByUserAndStatusIn(user, activeStatuses)
            .ifPresent(existing -> {
                throw new GdprExportException(
                    "An export request is already in progress (status: " + existing.getStatus() + "). " +
                    "Please wait for it to complete or expire before requesting a new one."
                );
            });

        // Guard: cooldown period
        exportRequestRepository.findTopByUserOrderByCreatedAtDesc(user)
            .ifPresent(last -> {
                Instant cooldownEnd = last.getCreatedAt().plus(cooldownDays, ChronoUnit.DAYS);
                if (Instant.now().isBefore(cooldownEnd)) {
                    long hoursLeft = ChronoUnit.HOURS.between(Instant.now(), cooldownEnd);
                    throw new GdprExportException(
                        "You can only request one export every " + cooldownDays + " days. " +
                        "Please wait approximately " + hoursLeft + " more hour(s)."
                    );
                }
            });

        // Create request
        GdprExportRequest exportRequest = GdprExportRequest.builder()
            .user(user)
            .status(GdprExportStatus.PENDING)
            .downloadToken(UUID.randomUUID().toString())
            .build();

        exportRequestRepository.save(exportRequest);
        log.info("GDPR export request created: token={}, user={}", exportRequest.getDownloadToken(), user.getUsername());

        // Publish event → listener sends confirmation email (async)
        Device device = deviceService.detectCurrentDevice();
        eventPublisher.publishEvent(
                new GdprExportRequestedEvent(
                    user,
                    exportRequest.getDownloadToken(),
                    device
                )
        );

        return exportRequest;
    }

    // =========================================================================
    // 2. Confirm
    // =========================================================================

    @Override
    @Transactional
    public void confirm(String token) {
        GdprExportRequest exportRequest = findValidPendingRequest(token);

        exportRequest.setStatus(GdprExportStatus.PROCESSING);
        exportRequest.setConfirmedAt(Instant.now());
        exportRequestRepository.save(exportRequest);

        log.info("GDPR export confirmed, triggering async generation: token={}", token);

        // Trigger async generation — runs on gdprExecutor
        generateArchiveAsync(exportRequest.getId());
    }

    // =========================================================================
    // 3. Generate (async)
    // =========================================================================

    @Override
    @Async("gdprExecutor")
    @Transactional
    public void generateArchiveAsync(Long requestId) {
        GdprExportRequest exportRequest = exportRequestRepository.findById(requestId)
            .orElseThrow(() -> new GdprExportException("Export request not found: " + requestId));

        User user = exportRequest.getUser();
        log.info("Starting async GDPR archive generation for user: {}", user.getUsername());

        try {
            // Collect all user data
            GdprUserDataSnapshot snapshot = dataCollectorService.collect(user);

            // Build ZIP archive
            byte[] zipBytes = buildZipArchive(snapshot);

            // Store archive
            storageService.save(exportRequest.getDownloadToken(), zipBytes);

            // Update request status
            Instant expiresAt = Instant.now().plus(archiveTtlHours, ChronoUnit.HOURS);
            exportRequest.setStatus(GdprExportStatus.READY);
            exportRequest.setGeneratedAt(Instant.now());
            exportRequest.setExpiresAt(expiresAt);
            exportRequestRepository.save(exportRequest);

            log.info("GDPR archive ready for user: {}, expires: {}", user.getUsername(), expiresAt);

            // Publish event → listener sends download-ready email (async)
            String downloadUrl = frontUrl + "/account/privacy?gdpr-token=" + exportRequest.getDownloadToken();
            eventPublisher.publishEvent(new GdprExportReadyEvent(user, exportRequest.getDownloadToken(), downloadUrl));

        } catch (Exception e) {
            log.error("GDPR archive generation failed for user: {}", user.getUsername(), e);
            exportRequest.setStatus(GdprExportStatus.FAILED);
            exportRequest.setFailureReason(e.getMessage());
            exportRequestRepository.save(exportRequest);
        }
    }

    // =========================================================================
    // 4. Download
    // =========================================================================

    @Override
    @Transactional
    public GdprDownloadResult download(String token) {
        GdprExportRequest exportRequest = exportRequestRepository.findByDownloadToken(token)
            .orElseThrow(() -> new GdprExportException("Invalid download token."));

        if (exportRequest.getStatus() == GdprExportStatus.DOWNLOADED) {
            throw new GdprExportException("This archive has already been downloaded.");
        }

        if (!exportRequest.isDownloadLinkValid()) {
            throw new GdprExportException("This download link has expired or is not yet ready.");
        }

        String filename = buildFilename(exportRequest.getUser());

        GdprDownloadResult result;
        if (storageService.isRemote()) {
            String presignedUrl = storageService.getDownloadUrl(token);
            result = GdprDownloadResult.redirect(presignedUrl, filename);
        } else {
            byte[] fileBytes = storageService.load(token);
            result = GdprDownloadResult.stream(fileBytes, filename);
        }

        // Mark as downloaded and delete archive
        exportRequest.setStatus(GdprExportStatus.DOWNLOADED);
        exportRequest.setDownloadedAt(Instant.now());
        exportRequestRepository.save(exportRequest);
        storageService.delete(token);

        log.info("GDPR archive downloaded and deleted: user={}", exportRequest.getUser().getUsername());

        return result;
    }

    // =========================================================================
    // 5. Status
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Optional<GdprExportRequest> getStatus(User user) {
        return exportRequestRepository.findTopByUserOrderByCreatedAtDesc(user);
    }

    // =========================================================================
    // Private — ZIP builder
    // =========================================================================

    /**
     * Builds a ZIP archive containing one pretty-printed JSON file per data domain.
     */
    private byte[] buildZipArchive(GdprUserDataSnapshot snapshot) throws IOException {
        ObjectMapper prettyMapper = objectMapper.copy()
            .enable(SerializationFeature.INDENT_OUTPUT);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {

            addJsonEntry(zip, prettyMapper, "profile.json",       snapshot.profile());
            addJsonEntry(zip, prettyMapper, "addresses.json",     snapshot.addresses());
            addJsonEntry(zip, prettyMapper, "devices.json",       snapshot.devices());
            addJsonEntry(zip, prettyMapper, "activity.json",      snapshot.activity());
            addJsonEntry(zip, prettyMapper, "notifications.json", snapshot.notifications());
            addJsonEntry(zip, prettyMapper, "calendar.json",      snapshot.calendar());
            addJsonEntry(zip, prettyMapper, "sport.json",         snapshot.sport());
            addJsonEntry(zip, prettyMapper, "meta.json",          snapshot.meta());
        }

        return baos.toByteArray();
    }

    private void addJsonEntry(ZipOutputStream zip, ObjectMapper mapper,
                               String filename, Object data) throws IOException {
        byte[] json = mapper.writeValueAsBytes(data);
        zip.putNextEntry(new ZipEntry(filename));
        zip.write(json);
        zip.closeEntry();
    }

    // =========================================================================
    // Private — helpers
    // =========================================================================

    private GdprExportRequest findValidPendingRequest(String token) {
        GdprExportRequest req = exportRequestRepository.findByDownloadToken(token)
            .orElseThrow(() -> new GdprExportException("Invalid or unknown token."));

        if (req.getStatus() != GdprExportStatus.PENDING) {
            throw new GdprExportException(
                "This token cannot be used: request status is " + req.getStatus()
            );
        }

        return req;
    }

    private String buildFilename(User user) {
        String date = LocalDate.now(ZoneOffset.UTC).toString(); // e.g. 2026-02-24
        return "gdpr-export-" + user.getUsername() + "-" + date + ".zip";
    }
}