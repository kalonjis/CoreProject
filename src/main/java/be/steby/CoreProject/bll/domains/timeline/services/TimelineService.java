package be.steby.CoreProject.bll.domains.timeline.services;

import be.steby.CoreProject.bll.domains.timeline.models.TimelineEntry;

import java.util.List;

/**
 * Service that builds the unified CRM activity timeline for a deal, contact, or lead.
 *
 * <h3>Unified timeline model</h3>
 * <p>The timeline merges two types of entries in reverse chronological order:</p>
 * <ul>
 *   <li>{@link be.steby.CoreProject.dl.entities.crm.Interaction} — spontaneous touchpoints
 *       (inbound calls, notes, contact forms, manual email logs, etc.)</li>
 *   <li>Completed {@link be.steby.CoreProject.dl.entities.crm.CommercialAction} — planned
 *       actions that have been marked as DONE (meetings held, tasks completed, etc.)</li>
 * </ul>
 *
 * <h3>SoC rationale</h3>
 * <p>This service intentionally lives in its own {@code timeline} domain rather than
 * inside {@code interaction} or {@code commercialaction}, because it is a cross-cutting
 * read concern that aggregates data from both domains without belonging to either.</p>
 */
public interface TimelineService {

    /**
     * Returns the unified activity timeline for a deal, most recent first.
     *
     * @param dealPublicId the public UUID of the deal
     * @return merged timeline entries (may be empty)
     * @throws IllegalArgumentException if the deal is not found
     */
    List<TimelineEntry> getTimelineByDeal(String dealPublicId);

    /**
     * Returns the unified activity timeline for a contact, most recent first.
     *
     * @param contactPublicId the public UUID of the contact
     * @return merged timeline entries (may be empty)
     * @throws IllegalArgumentException if the contact is not found
     */
    List<TimelineEntry> getTimelineByContact(String contactPublicId);

    /**
     * Returns the unified activity timeline for a lead, most recent first.
     *
     * <p>Used during lead qualification — before conversion to a Contact.</p>
     *
     * @param leadPublicId the public UUID of the lead
     * @return merged timeline entries (may be empty)
     * @throws IllegalArgumentException if the lead is not found
     */
    List<TimelineEntry> getTimelineByLead(String leadPublicId);
}
