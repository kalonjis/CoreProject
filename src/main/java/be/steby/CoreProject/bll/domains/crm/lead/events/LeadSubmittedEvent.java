package be.steby.CoreProject.bll.domains.crm.lead.events;

import be.steby.CoreProject.dl.entities.crm.Lead;

/**
 * Event published when a public inquiry is submitted successfully.
 *
 * <p>Used to trigger email notifications to the appropriate team
 * and acknowledgment to the visitor.</p>
 *
 * @param lead The persisted inquiry (without message content)
 * @param message The message content (not persisted, only for email)
 */
public record LeadSubmittedEvent(
        Lead lead,
        String message
) {

    /**
     * Gets the email address to reply to.
     *
     * @return the visitor's email address
     */
    public String getReplyToEmail() {
        return lead.getEmail();
    }

    /**
     * Gets the visitor's name, or "Anonymous" if not provided.
     *
     * @return the name or default value
     */
    public String getVisitorName() {
        return lead.getDisplayName().orElse("Anonymous");
    }

    /**
     * Gets the subject line for the notification email.
     *
     * @return formatted subject
     */
    public String getEmailSubject() {
        return String.format("[%s] %s",
                lead.getLeadType().name(),
                lead.getSubject()
        );
    }
}