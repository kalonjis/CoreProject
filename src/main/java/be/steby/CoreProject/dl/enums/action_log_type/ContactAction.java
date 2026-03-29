package be.steby.CoreProject.dl.enums.action_log_type;

/**
 * CRM contact domain actions.
 *
 * <p>Each constant maps 1-to-1 to a contact domain event:
 * <ul>
 *   <li>{@code CONTACT_CREATED}           ← ContactCreatedEvent</li>
 *   <li>{@code CONTACT_CREATED_FROM_LEAD} ← ContactCreatedFromLeadEvent</li>
 *   <li>{@code CONTACT_STATUS_CHANGED}    ← ContactStatusChangedEvent</li>
 *   <li>{@code CONTACT_ASSIGNED}          ← ContactAssignedEvent</li>
 *   <li>{@code CONTACT_MERGED}            ← ContactMergedEvent</li>
 *   <li>{@code CONTACT_ORG_LINKED}        ← ContactLinkedToOrganisationEvent</li>
 *   <li>{@code CONTACT_ORG_UNLINKED}      ← ContactUnlinkedFromOrganisationEvent</li>
 *   <li>{@code CONTACT_ARCHIVED}          ← ContactArchivedEvent</li>
 * </ul>
 */
public enum ContactAction implements ActionLogType {

    // =========================================================================
    // Creation
    // =========================================================================

    CONTACT_CREATED("Contact created manually"),
    CONTACT_CREATED_FROM_LEAD("Contact created from lead conversion"),

    // =========================================================================
    // Status
    // =========================================================================

    CONTACT_STATUS_CHANGED("Contact status changed"),

    // =========================================================================
    // Assignment
    // =========================================================================

    CONTACT_ASSIGNED("Contact assigned to a commercial"),

    // =========================================================================
    // Merge
    // =========================================================================

    CONTACT_MERGED("Contact merged into another contact"),

    // =========================================================================
    // Organisation
    // =========================================================================

    CONTACT_ORG_LINKED("Contact linked to an organisation"),
    CONTACT_ORG_UNLINKED("Contact unlinked from an organisation"),

    // =========================================================================
    // Archive
    // =========================================================================

    CONTACT_ARCHIVED("Contact archived");

    // =========================================================================

    private final String description;

    ContactAction(String description) {
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
        return "CRM_CONTACT";
    }
}
