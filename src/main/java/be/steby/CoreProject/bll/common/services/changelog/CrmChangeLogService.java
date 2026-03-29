package be.steby.CoreProject.bll.common.services.changelog;

import be.steby.CoreProject.bll.common.models.changelog.FieldChange;
import be.steby.CoreProject.dal.repositories.crm.CrmChangeLogRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CrmChangeLog;
import be.steby.CoreProject.dl.enums.crm.CrmEntityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Abstract base service for persisting CRM field change log entries.
 *
 * <p>All writes are asynchronous (fire-and-forget on the {@code activityLogExecutor}
 * thread pool) and wrapped in a try/catch so that a logging failure never
 * propagates to the caller. Domain-specific subclasses extend this class and
 * call {@link #logChange} or {@link #logChanges} with the appropriate context.</p>
 *
 * <p>Each write runs in its own transaction, independent of the caller's
 * business transaction. This ensures that changelog entries are only persisted
 * after the main transaction commits successfully.</p>
 */
@RequiredArgsConstructor
@Slf4j
public abstract class CrmChangeLogService {

    private final CrmChangeLogRepository crmChangeLogRepository;

    /**
     * Persists a single field change entry asynchronously.
     *
     * @param entityType     the CRM entity type
     * @param entityPublicId the public identifier of the changed entity
     * @param fieldName      name of the changed field
     * @param oldValue       previous value as string; null if the field had no prior value
     * @param newValue       new value as string
     * @param changedBy      the user who made the change; null for system-triggered changes
     */
    @Async("activityLogExecutor")
    @Transactional
    public void logChange(CrmEntityType entityType, String entityPublicId,
                          String fieldName, String oldValue, String newValue,
                          User changedBy) {
        try {
            buildAndSave(entityType, entityPublicId, fieldName, oldValue, newValue, changedBy);
        } catch (Exception e) {
            log.error("Failed to persist CRM change log — entity: {}/{}, field: {}",
                    entityType, entityPublicId, fieldName, e);
        }
    }

    /**
     * Persists multiple field change entries for the same entity in a single async call.
     *
     * <p>Prefer this over calling {@link #logChange} in a loop when several fields
     * change in one service operation — it batches all saves in one transaction.</p>
     *
     * <p>Entries with identical {@code oldValue} and {@code newValue} are silently
     * skipped — no point recording a no-op change.</p>
     *
     * @param entityType     the CRM entity type
     * @param entityPublicId the public identifier of the changed entity
     * @param changes        list of field changes to persist; empty list is a no-op
     * @param changedBy      the user who made the change; null for system-triggered changes
     */
    @Async("activityLogExecutor")
    @Transactional
    public void logChanges(CrmEntityType entityType, String entityPublicId,
                           List<FieldChange> changes, User changedBy) {
        if (changes == null || changes.isEmpty()) return;
        try {
            for (FieldChange change : changes) {
                if (isNoOp(change)) continue;
                buildAndSave(entityType, entityPublicId, change.fieldName(),
                        change.oldValue(), change.newValue(), changedBy);
            }
        } catch (Exception e) {
            log.error("Failed to persist CRM change log batch — entity: {}/{}, changes: {}",
                    entityType, entityPublicId, changes.size(), e);
        }
    }

    /**
     * Returns the CRM domain name used for debug logging.
     * Implemented by each domain-specific subclass.
     *
     * @return domain name string, e.g. {@code "CONTACT"}, {@code "DEAL"}
     */
    protected abstract String getDomainName();

    // ─── Private ──────────────────────────────────────────────────────────────

    /**
     * Builds and saves a single {@link CrmChangeLog} entry.
     */
    private void buildAndSave(CrmEntityType entityType, String entityPublicId,
                               String fieldName, String oldValue, String newValue,
                               User changedBy) {
        CrmChangeLog entry = CrmChangeLog.builder()
                .entityType(entityType)
                .entityPublicId(entityPublicId)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(changedBy)
                .build();

        crmChangeLogRepository.save(entry);

        log.debug("[{}] Change logged — entity: {}, field: {} ({} → {})",
                getDomainName(), entityPublicId, fieldName, oldValue, newValue);
    }

    /**
     * Returns {@code true} if the old and new values are identical (no actual change).
     */
    private boolean isNoOp(FieldChange change) {
        if (change.oldValue() == null && change.newValue() == null) return true;
        if (change.oldValue() == null) return false;
        return change.oldValue().equals(change.newValue());
    }
}
