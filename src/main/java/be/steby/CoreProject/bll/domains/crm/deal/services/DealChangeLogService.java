package be.steby.CoreProject.bll.domains.crm.deal.services;

import be.steby.CoreProject.bll.common.services.changelog.CrmChangeLogService;
import be.steby.CoreProject.dal.repositories.crm.CrmChangeLogRepository;
import org.springframework.stereotype.Service;

/**
 * Deal domain change log service.
 *
 * <p>Delegates all persistence to the generic {@link CrmChangeLogService} base.
 * Callers (e.g. {@code DealServiceImpl}) inject this bean and call
 * {@code logChange} / {@code logChanges} directly — no event listener needed
 * since change logging is a single-consumer concern in this domain.</p>
 */
@Service
public class DealChangeLogService extends CrmChangeLogService {

    /**
     * Constructs the service with the shared change log repository.
     *
     * @param crmChangeLogRepository the shared CRM change log repository
     */
    public DealChangeLogService(CrmChangeLogRepository crmChangeLogRepository) {
        super(crmChangeLogRepository);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "DEAL"}
     */
    @Override
    protected String getDomainName() {
        return "DEAL";
    }
}
