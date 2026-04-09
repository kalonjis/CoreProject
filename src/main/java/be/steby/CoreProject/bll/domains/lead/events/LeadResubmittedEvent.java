package be.steby.CoreProject.bll.domains.lead.events;

import be.steby.CoreProject.dl.entities.crm.Lead;

/**
 * Event published when a public form submission is received for an email address
 * that already has an active lead (NEW or IN_REVIEW) in the CRM.
 *
 * <p>No new lead is created — the existing one is returned. This event carries
 * the new submission's content so downstream listeners can log a system interaction
 * on the existing lead, keeping its timeline up to date.</p>
 *
 * @param existingLead the existing active lead that absorbed the duplicate submission
 * @param newSubject   subject field of the new submission
 * @param newMessage   message body of the new submission, or {@code null} if absent
 */
public record LeadResubmittedEvent(
        Lead existingLead,
        String newSubject,
        String newMessage
) {}
