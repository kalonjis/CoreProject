package be.steby.CoreProject.bll.domains.gdpr.services.scheduler;

import be.steby.CoreProject.bll.domains.gdpr.services.storage.GdprStorageService;
import be.steby.CoreProject.dal.repositories.GdprExportRequestRepository;
import be.steby.CoreProject.dl.entities.GdprExportRequest;
import be.steby.CoreProject.dl.enums.GdprExportStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled cleanup job for expired GDPR export archives.
 *
 * <p>Runs every hour and:
 * <ul>
 *   <li>Finds READY or DOWNLOADED requests whose TTL has elapsed</li>
 *   <li>Deletes the physical ZIP file from storage</li>
 *   <li>Marks the request as EXPIRED</li>
 * </ul>
 *
 * <p>This job is solely responsible for physical archive deletion.
 * The download endpoint no longer deletes files immediately, allowing
 * users to re-download their archive within the TTL window.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GdprCleanupScheduler {

    private final GdprExportRequestRepository exportRequestRepository;
    private final GdprStorageService           storageService;

    /**
     * Expires and deletes archives whose TTL has elapsed.
     * Runs every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredArchives() {
        Instant now = Instant.now();
        log.info("Running GDPR archive cleanup at {}", now);

        List<GdprExportRequest> expired = exportRequestRepository.findExpiredRequests(now);

        if (expired.isEmpty()) {
            log.debug("No expired GDPR archives to clean up");
            return;
        }

        int cleaned = 0;
        for (GdprExportRequest req : expired) {
            try {
                storageService.delete(req.getDownloadToken());
                req.setStatus(GdprExportStatus.EXPIRED);
                exportRequestRepository.save(req);
                cleaned++;
                log.debug("GDPR archive expired and deleted: user={}, token={}",
                    req.getUser().getUsername(), req.getDownloadToken());
            } catch (Exception e) {
                log.warn("Failed to clean up GDPR archive for token={}: {}",
                    req.getDownloadToken(), e.getMessage());
            }
        }

        log.info("GDPR cleanup complete: {}/{} archives expired", cleaned, expired.size());
    }
}