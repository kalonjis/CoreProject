package be.steby.CoreProject.bll.domains.timeline.models;

import be.steby.CoreProject.dl.entities.crm.CommercialAction;
import be.steby.CoreProject.dl.entities.crm.Interaction;

import java.time.Instant;

/**
 * Unified timeline entry representing either a logged {@link Interaction}
 * or a completed {@link CommercialAction}.
 *
 * <h3>Purpose</h3>
 * <p>The CRM timeline displays all touchpoints in reverse chronological order,
 * regardless of whether they were spontaneous interactions (calls, notes, emails)
 * or planned commercial actions that were completed.
 * This sealed interface provides a common type for the merged, sorted list
 * produced by {@link be.steby.CoreProject.bll.domains.timeline.services.TimelineService}.</p>
 *
 * <h3>Date field used for sorting</h3>
 * <ul>
 *   <li>{@link InteractionEntry} → {@code interaction.occurredAt}</li>
 *   <li>{@link CommercialActionEntry} → {@code action.completedAt}</li>
 * </ul>
 */
public sealed interface TimelineEntry permits TimelineEntry.InteractionEntry, TimelineEntry.CommercialActionEntry {

    /**
     * Returns the reference timestamp used for chronological ordering.
     *
     * @return the occurrence or completion instant
     */
    Instant occurredAt();

    // =========================================================================
    // Permitted implementations
    // =========================================================================

    /**
     * A timeline entry backed by a spontaneous {@link Interaction}
     * (note, inbound call, contact form, etc.).
     *
     * @param interaction the underlying interaction entity
     */
    record InteractionEntry(Interaction interaction) implements TimelineEntry {
        @Override
        public Instant occurredAt() {
            return interaction.getOccurredAt();
        }
    }

    /**
     * A timeline entry backed by a completed {@link CommercialAction}
     * (MEETING done, CALL completed, TASK closed, etc.).
     *
     * @param action the underlying commercial action entity (status must be DONE)
     */
    record CommercialActionEntry(CommercialAction action) implements TimelineEntry {
        @Override
        public Instant occurredAt() {
            return action.getCompletedAt();
        }
    }
}
