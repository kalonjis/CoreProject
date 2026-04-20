package be.steby.CoreProject.bll.domains.crm.organisation.services;

import be.steby.CoreProject.bll.common.services.changelog.CrmChangeLogService;
import be.steby.CoreProject.dal.repositories.crm.CrmChangeLogRepository;
import org.springframework.stereotype.Service;

/**
 * Organisation domain change log service.
 *
 * <p>Delegates all persistence to the generic {@link CrmChangeLogService} base.
 * Callers (e.g. {@code OrganisationServiceImpl}) inject this bean and call
 * {@code logChange} / {@code logChanges} directly — no event listener needed
 * since change logging is a single-consumer concern in this domain.</p>
 */
@Service
public class OrganisationChangeLogService extends CrmChangeLogService {

    /**
     * Constructs the service with the shared change log repository.
     *
     * @param crmChangeLogRepository the shared CRM change log repository
     */
    public OrganisationChangeLogService(CrmChangeLogRepository crmChangeLogRepository) {
        super(crmChangeLogRepository);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "ORGANISATION"}
     */
    @Override
    protected String getDomainName() {
        return "ORGANISATION";
    }
}
