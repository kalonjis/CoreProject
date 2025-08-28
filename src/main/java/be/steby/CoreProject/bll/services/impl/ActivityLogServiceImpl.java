package be.steby.CoreProject.bll.services.impl;

import be.steby.CoreProject.bll.services.ActivityLogService;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Service commun UNIQUEMENT pour des cas cross-domain rares
 * PAS de méthodes de logging - tout est dans les domaines !
 */
@Service("commonActivityLogService")
@RequiredArgsConstructor
@Slf4j
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    // ================== QUERIES TRANSVERSALES SEULEMENT ==================

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLog> getUserConnectionHistory(User user, Pageable pageable) {
        return activityLogRepository.findByUserOrderByTimestampDesc(user, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLog> searchLogs(Long userId, String ipAddress,
                                        Instant startDate, Instant endDate, Pageable pageable) {
        return activityLogRepository.findBySearchCriteria(
                userId, ipAddress, startDate, endDate, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSystemStats(Instant startDate, Instant endDate) {
        return activityLogRepository.getSystemStats(startDate, endDate);
    }

    @Override
    @Transactional
    public void cleanupOldLogs() {
        Instant cutoffDate = Instant.now().minusSeconds(365 * 24 * 3600); // 1 an
        int deleted = activityLogRepository.deleteByTimestampBefore(cutoffDate);
        log.info("Cleaned up {} activity logs older than {}", deleted, cutoffDate);
    }
}