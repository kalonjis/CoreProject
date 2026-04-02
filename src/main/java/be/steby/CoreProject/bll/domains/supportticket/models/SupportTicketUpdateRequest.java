package be.steby.CoreProject.bll.domains.supportticket.models;

/**
 * BLL request model for partially updating a support ticket's content fields.
 *
 * <p>{@code subject} is applied only when non-null.
 * {@code description} is always applied — pass {@code null} to clear it.</p>
 *
 * @param subject     new subject line (optional, ignored when null)
 * @param description new description, or {@code null} to clear it
 */
public record SupportTicketUpdateRequest(
        String subject,
        String description
) {}
