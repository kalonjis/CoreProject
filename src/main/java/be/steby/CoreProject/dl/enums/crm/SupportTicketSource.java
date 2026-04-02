package be.steby.CoreProject.dl.enums.crm;

/**
 * Indicates how a {@link be.steby.CoreProject.dl.entities.crm.SupportTicket} was created.
 *
 * <ul>
 *   <li>{@code INTERNAL} — created manually by a team member via the CRM</li>
 *   <li>{@code PUBLIC_FORM} — submitted by a visitor through the public contact form</li>
 * </ul>
 */
public enum SupportTicketSource {
    INTERNAL,
    PUBLIC_FORM
}
