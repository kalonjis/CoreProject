package be.steby.CoreProject.bll.common.services.changelog;

import be.steby.CoreProject.bll.common.models.changelog.CrmChangeLogFilter;
import be.steby.CoreProject.dal.repositories.crm.CrmChangeLogRepository;
import be.steby.CoreProject.dal.specifications.crm.CrmChangeLogSpecification;
import be.steby.CoreProject.dl.entities.crm.CrmChangeLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only service for querying CRM change log entries.
 *
 * <p>Intentionally separated from {@link CrmChangeLogService} (write side) to
 * respect CQRS: writes are async / fire-and-forget; reads are synchronous,
 * transactional and called from the controller layer.</p>
 *
 * <p>All filtering is delegated to {@link CrmChangeLogSpecification} via
 * {@link CrmChangeLogFilter} — a single {@code repository.findAll(spec, pageable)}
 * call covers every combination.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CrmChangeLogQueryService {

    private final CrmChangeLogRepository crmChangeLogRepository;

    /**
     * Returns a paginated list of change log entries matching the given filter,
     * sorted by {@code changedAt} descending (most recent first).
     *
     * @param filter   BLL filter built from the controller request; all fields optional
     * @param pageable pagination and sort parameters
     * @return page of matching {@link CrmChangeLog} entries
     */
    public Page<CrmChangeLog> getChanges(CrmChangeLogFilter filter, Pageable pageable) {
        log.debug("Fetching change log — entityType: {}, entityPublicId: {}, from: {}, to: {}",
                filter.entityType(), filter.entityPublicId(), filter.from(), filter.to());

        return crmChangeLogRepository.findAll(
                CrmChangeLogSpecification.build(filter), pageable);
    }

    /**
     * Returns the most recent change log entries across all CRM entities,
     * with no entity filter applied.
     *
     * <p>Intended for global views: dashboard widgets and the audit log page.
     * Sorting is delegated to the {@code pageable} — callers should pass
     * {@code changedAt DESC}.</p>
     *
     * @param pageable pagination and sort parameters
     * @return page of the most recent {@link CrmChangeLog} entries
     */
    public Page<CrmChangeLog> getRecent(Pageable pageable) {
        log.debug("Fetching recent change log — page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return crmChangeLogRepository.findAll(pageable);
    }
}
