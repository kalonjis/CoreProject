package be.steby.CoreProject.dl.enums.action_log_type;

/**
 * CRM lead domain actions.
 *
 * <p>Each constant maps 1-to-1 to a lead domain event:
 * <ul>
 *   <li>{@code LEAD_SUBMITTED} ← LeadSubmittedEvent  (public form — no authenticated user)</li>
 *   <li>{@code LEAD_ASSIGNED}  ← LeadAssignedEvent</li>
 *   <li>{@code LEAD_IN_REVIEW} ← LeadInReviewEvent</li>
 *   <li>{@code LEAD_CONVERTED} ← LeadConvertedEvent</li>
 *   <li>{@code LEAD_REJECTED}  ← LeadRejectedEvent</li>
 * </ul>
 */
public enum LeadAction implements ActionLogType {

    // =========================================================================
    // Submission
    // =========================================================================

    /**
     * Logged when a lead is submitted via public contact form or webhook.
     * No authenticated user — {@code user} will be null in the activity log.
     */
    LEAD_SUBMITTED("Lead submitted via public form or webhook"),

    // =========================================================================
    // Lifecycle
    // =========================================================================

    LEAD_ASSIGNED("Lead assigned to a commercial"),
    LEAD_IN_REVIEW("Lead marked as in review"),
    LEAD_CONVERTED("Lead converted to contact"),
    LEAD_REJECTED("Lead rejected");

    // =========================================================================

    private final String description;

    LeadAction(String description) {
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
        return "CRM_LEAD";
    }
}
