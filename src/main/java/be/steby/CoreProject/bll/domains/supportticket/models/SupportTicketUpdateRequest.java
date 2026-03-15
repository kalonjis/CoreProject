package be.steby.CoreProject.bll.domains.supportticket.models;

/**
 * BLL request model for partially updating a support ticket's content fields.
 *
 * <p>Null fields are ignored — only non-null values are applied.</p>
 *
 * @param subject     new subject line (optional)
 * @param description new description (optional)
 */
public record SupportTicketUpdateRequest(
        String subject,
        String description
) {}
