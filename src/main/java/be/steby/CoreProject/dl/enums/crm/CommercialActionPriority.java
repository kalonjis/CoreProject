package be.steby.CoreProject.dl.enums.crm;

import be.steby.CoreProject.dl.entities.crm.CommercialAction;

/**
 * Priority level of a {@link CommercialAction}.
 *
 * <p>Used to sort the commercial's task list and to visually highlight
 * urgent items in the UI (e.g. red badge for {@code HIGH}).</p>
 */
public enum CommercialActionPriority {

    /** Can be done whenever time permits. */
    LOW,

    /** Should be done within the normal workday. Default value. */
    MEDIUM,

    /** Must be done as soon as possible — shown prominently in the UI. */
    HIGH
}