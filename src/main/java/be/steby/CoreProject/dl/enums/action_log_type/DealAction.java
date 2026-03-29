package be.steby.CoreProject.dl.enums.action_log_type;

/**
 * CRM deal domain actions.
 *
 * <p>Each constant maps 1-to-1 to a deal domain event:
 * <ul>
 *   <li>{@code DEAL_CREATED}      ← DealCreatedEvent</li>
 *   <li>{@code DEAL_STAGE_MOVED}  ← DealStageChangedEvent</li>
 *   <li>{@code DEAL_WON}          ← DealWonEvent</li>
 *   <li>{@code DEAL_LOST}         ← DealLostEvent</li>
 *   <li>{@code DEAL_REASSIGNED}   ← DealReassignedEvent</li>
 * </ul>
 */
public enum DealAction implements ActionLogType {

    // =========================================================================
    // Creation
    // =========================================================================

    DEAL_CREATED("Deal created"),

    // =========================================================================
    // Stage
    // =========================================================================

    DEAL_STAGE_MOVED("Deal moved to a new pipeline stage"),
    DEAL_WON("Deal marked as won"),
    DEAL_LOST("Deal marked as lost"),

    // =========================================================================
    // Assignment
    // =========================================================================

    DEAL_REASSIGNED("Deal reassigned to a commercial");

    // =========================================================================

    private final String description;

    DealAction(String description) {
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
        return "CRM_DEAL";
    }
}
