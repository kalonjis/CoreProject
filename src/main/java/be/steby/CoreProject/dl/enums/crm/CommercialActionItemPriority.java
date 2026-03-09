package be.steby.CoreProject.dl.enums.crm;

/**
 * Priority level of a {@link be.steby.CoreProject.dl.entities.crm.CommercialActionItem}.
 *
 * <p>Used to sort the commercial's task list and to visually highlight
 * urgent items in the UI (e.g. red badge for {@code HIGH}).</p>
 */
public enum CommercialActionItemPriority {

    /** Can be done whenever time permits. */
    LOW,

    /** Should be done within the normal workday. Default value. */
    MEDIUM,

    /** Must be done as soon as possible — shown prominently in the UI. */
    HIGH
}