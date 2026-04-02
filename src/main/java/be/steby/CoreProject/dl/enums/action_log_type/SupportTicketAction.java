package be.steby.CoreProject.dl.enums.action_log_type;

/**
 * CRM support ticket domain actions.
 *
 * <p>Each constant maps 1-to-1 to a support ticket domain event:
 * <ul>
 *   <li>{@code TICKET_CREATED}        ← SupportTicketCreatedEvent</li>
 *   <li>{@code TICKET_STATUS_CHANGED} ← SupportTicketStatusChangedEvent</li>
 *   <li>{@code TICKET_ASSIGNED}       ← SupportTicketAssignedEvent</li>
 *   <li>{@code TICKET_CLOSED}         ← SupportTicketClosedEvent</li>
 * </ul>
 */
public enum SupportTicketAction implements ActionLogType {

    // =========================================================================
    // Creation
    // =========================================================================

    TICKET_CREATED("Support ticket created"),

    // =========================================================================
    // Lifecycle
    // =========================================================================

    TICKET_STATUS_CHANGED("Support ticket status changed"),
    TICKET_CLOSED("Support ticket closed"),

    // =========================================================================
    // Assignment
    // =========================================================================

    TICKET_ASSIGNED("Support ticket assigned to a team member"),

    // =========================================================================
    // Deletion
    // =========================================================================

    TICKET_DELETED("Support ticket deleted");

    // =========================================================================

    private final String description;

    SupportTicketAction(String description) {
        this.description = description;
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return "CRM_SUPPORT";
    }
}
