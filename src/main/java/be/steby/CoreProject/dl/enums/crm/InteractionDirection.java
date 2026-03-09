package be.steby.CoreProject.dl.enums.crm;

/**
 * Direction of an interaction relative to the company.
 *
 * <p>Only relevant for {@code CALL} and {@code EMAIL} interaction types.
 * Not applicable for notes, meetings, or visits.</p>
 */
public enum InteractionDirection {

    /** The commercial initiated the action toward the contact. */
    OUTBOUND,

    /** The contact initiated the action toward the company. */
    INBOUND
}