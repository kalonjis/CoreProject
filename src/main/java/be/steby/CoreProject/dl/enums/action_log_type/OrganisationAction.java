package be.steby.CoreProject.dl.enums.action_log_type;

/**
 * CRM organisation domain actions.
 *
 * <p>Each constant maps 1-to-1 to an organisation domain event:
 * <ul>
 *   <li>{@code ORGANISATION_CREATED}  ← OrganisationCreatedEvent</li>
 *   <li>{@code ORGANISATION_ARCHIVED} ← OrganisationArchivedEvent</li>
 *   <li>{@code ORGANISATION_MERGED}   ← OrganisationMergedEvent</li>
 * </ul>
 */
public enum OrganisationAction implements ActionLogType {

    // =========================================================================
    // Creation
    // =========================================================================

    ORGANISATION_CREATED("Organisation created"),

    // =========================================================================
    // Archive
    // =========================================================================

    ORGANISATION_ARCHIVED("Organisation archived"),

    // =========================================================================
    // Merge
    // =========================================================================

    ORGANISATION_MERGED("Organisation merged into another organisation");

    // =========================================================================

    private final String description;

    OrganisationAction(String description) {
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
        return "CRM_ORGANISATION";
    }
}
