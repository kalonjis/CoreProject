package be.steby.CoreProject.bll.common.models.changelog;

import be.steby.CoreProject.dl.enums.crm.CrmEntityType;

import java.time.Instant;

/**
 * BLL-level filter for querying {@code CrmChangeLog} entries.
 *
 * <p>All fields are optional — null values are treated as "no constraint"
 * by {@code CrmChangeLogSpecification}. This record is built by the controller
 * from the incoming {@code CrmChangeLogFilterRequest} and passed to
 * {@code CrmChangeLogQueryService}.</p>
 *
 * @param entityType      the CRM entity type to filter on; null means all types
 * @param entityPublicId  the public identifier of the target entity; null means all entities
 * @param from            lower bound on {@code changedAt} (inclusive); null means no lower bound
 * @param to              upper bound on {@code changedAt} (inclusive); null means no upper bound
 */
public record CrmChangeLogFilter(
        CrmEntityType entityType,
        String entityPublicId,
        Instant from,
        Instant to
) {
}
