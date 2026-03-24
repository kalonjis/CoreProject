package be.steby.CoreProject.dl.enums.crm;

/**
 * Type of a {@link be.steby.CoreProject.dl.entities.crm.CommercialAction}.
 *
 * <p>Determines the nature of the work to be done and whether
 * the action should block a time slot in the commercial's calendar.</p>
 *
 * <h3>Calendar integration</h3>
 * <p>Types for which {@link #requiresCalendarSlot()} returns {@code true}
 * ({@code MEETING}, {@code DEMO}) automatically trigger the creation of a
 * linked {@link be.steby.CoreProject.dl.entities.CalendarEvent} when the
 * action is created with a {@code dueDate}.</p>
 */
public enum CommercialActionType {

    /** Generic to-do item not covered by a more specific type. */
    TASK,

    /** Outbound or inbound phone call to perform or log. */
    CALL,

    /** Email to draft and send to the contact. */
    EMAIL,

    /**
     * In-person or remote meeting with the contact.
     *
     * <p>Triggers automatic calendar event creation when {@code dueDate} is set.</p>
     */
    MEETING,

    /**
     * Product or service demonstration with the contact.
     *
     * <p>Triggers automatic calendar event creation when {@code dueDate} is set.</p>
     */
    DEMO;

    /**
     * Returns {@code true} if this action type requires a dedicated time slot
     * in the commercial's calendar.
     *
     * @return {@code true} for {@code MEETING} and {@code DEMO}
     */
    public boolean requiresCalendarSlot() {
        return this == MEETING || this == DEMO;
    }
}
