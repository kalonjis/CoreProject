package be.steby.CoreProject.bll.common.models.changelog;

/**
 * Represents a single field-level change to be persisted as a {@code CrmChangeLog} entry.
 *
 * <p>Used as a lightweight value object when multiple fields change in one
 * service call — the caller builds a list of {@code FieldChange} instances
 * and passes them to {@code CrmChangeLogService.logChanges()} in a single
 * async invocation.</p>
 *
 * @param fieldName  name of the changed field (e.g. {@code "email"}, {@code "status"})
 * @param oldValue   string representation of the previous value; null if the field had no prior value
 * @param newValue   string representation of the new value
 */
public record FieldChange(String fieldName, String oldValue, String newValue) {
}
