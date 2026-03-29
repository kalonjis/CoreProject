package be.steby.CoreProject.pl.domains.changelog.models.requests;

import be.steby.CoreProject.bll.common.models.changelog.CrmChangeLogFilter;
import be.steby.CoreProject.dl.enums.crm.CrmEntityType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

/**
 * HTTP request model for filtering CRM change log entries.
 *
 * <p>All fields are optional — omitted fields produce no filter constraint.
 * Converted to {@link CrmChangeLogFilter} before being passed to the BLL.</p>
 *
 * @param entityType      CRM entity type to filter on (path variable, mapped by controller)
 * @param entityPublicId  public identifier of the entity (path variable, mapped by controller)
 * @param from            lower bound on {@code changedAt}, ISO-8601 instant; null means no lower bound
 * @param to              upper bound on {@code changedAt}, ISO-8601 instant; null means no upper bound
 */
public record CrmChangeLogFilterRequest(
        CrmEntityType entityType,
        String entityPublicId,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant from,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant to
) {

    /**
     * Converts this presentation-layer request to a BLL-level filter.
     *
     * @return a {@link CrmChangeLogFilter} with the same field values
     */
    public CrmChangeLogFilter toFilter() {
        return new CrmChangeLogFilter(entityType, entityPublicId, from, to);
    }
}
