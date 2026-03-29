package be.steby.CoreProject.bll.domains.outreach.models;

/**
 * BLL request model for sending a CRM outreach email to a contact.
 *
 * @param contactPublicId public UUID of the contact to send the email to (required)
 * @param dealPublicId    public UUID of the deal to attach the interaction log to (optional)
 * @param subject         email subject written by the commercial (required)
 * @param body            email body written by the commercial (required)
 */
public record CrmOutreachRequest(
        String contactPublicId,
        String dealPublicId,
        String subject,
        String body
) {}
