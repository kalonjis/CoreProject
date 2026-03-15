package be.steby.CoreProject.bll.domains.supportticket.models;

/**
 * BLL request model for creating a new support ticket.
 *
 * @param subject                short subject line of the ticket (required)
 * @param description            detailed description of the issue (optional)
 * @param submittedByPublicId    public UUID of the contact submitting the ticket (required)
 * @param assignedToPublicId     public UUID of the user to assign the ticket to (optional)
 */
public record SupportTicketCreateRequest(
        String subject,
        String description,
        String submittedByPublicId,
        String assignedToPublicId
) {}
