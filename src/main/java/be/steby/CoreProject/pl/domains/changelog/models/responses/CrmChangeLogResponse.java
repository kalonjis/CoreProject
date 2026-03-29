package be.steby.CoreProject.pl.domains.changelog.models.responses;

import be.steby.CoreProject.dl.entities.crm.CrmChangeLog;
import be.steby.CoreProject.dl.enums.crm.CrmEntityType;

import java.time.Instant;

/**
 * API response model for a single CRM change log entry.
 *
 * <p>Maps from {@link CrmChangeLog} — only exposes public fields;
 * the internal {@code id} is never included.</p>
 *
 * @param publicId        public UUID of the log entry
 * @param entityType      CRM domain this entry belongs to
 * @param entityPublicId  public identifier of the changed entity; null when anonymised
 * @param fieldName       name of the changed field
 * @param oldValue        previous value; null when the field had no prior value or after anonymisation
 * @param newValue        new value; null after anonymisation
 * @param changedBy       username of the actor; null for system-triggered changes
 * @param changedAt       UTC timestamp of the change
 */
public record CrmChangeLogResponse(
        String publicId,
        CrmEntityType entityType,
        String entityPublicId,
        String fieldName,
        String oldValue,
        String newValue,
        String changedBy,
        Instant changedAt
) {

    /**
     * Maps a {@link CrmChangeLog} entity to its API response representation.
     *
     * @param log the change log entry to map; must not be null
     * @return the corresponding response record
     */
    public static CrmChangeLogResponse from(CrmChangeLog log) {
        return new CrmChangeLogResponse(
                log.getPublicId(),
                log.getEntityType(),
                log.getEntityPublicId(),
                log.getFieldName(),
                log.getOldValue(),
                log.getNewValue(),
                log.getChangedBy() != null ? log.getChangedBy().getUsername() : null,
                log.getChangedAt()
        );
    }
}
