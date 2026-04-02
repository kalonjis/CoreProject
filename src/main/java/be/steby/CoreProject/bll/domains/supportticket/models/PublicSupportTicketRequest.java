package be.steby.CoreProject.bll.domains.supportticket.models;

/**
 * BLL model for a support ticket submitted via the public contact form.
 *
 * <p>If a Contact with the given email already exists, the ticket is linked to it.
 * Otherwise firstName, lastName and email are stored as {@code reporterName}/{@code reporterEmail}
 * directly on the ticket — no Contact is created.</p>
 *
 * @param firstName   first name of the submitter (required)
 * @param lastName    last name of the submitter (required)
 * @param email       email address of the submitter (required)
 * @param subject     short subject of the issue (required)
 * @param description detailed description of the issue (optional)
 * @param website     honeypot field — must be empty (bots fill this)
 */
public record PublicSupportTicketRequest(
        String firstName,
        String lastName,
        String email,
        String subject,
        String description,
        String website
) {
    public boolean isHoneypotFilled() {
        return website != null && !website.isBlank();
    }
}
