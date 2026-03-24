package be.steby.CoreProject.dl.enums.crm;

import be.steby.CoreProject.dl.entities.crm.Interaction;

/**
 * Type of action recorded in a {@link Interaction}.
 *
 * <p>Determines the icon shown in the CRM timeline and whether a
 * {@link be.steby.CoreProject.dl.entities.crm.CallLog} or
 * {@link be.steby.CoreProject.dl.entities.crm.EmailLog} detail record is expected.</p>
 */
public enum InteractionType {

    /** Phone call — expects a {@code CallLog} sub-entity. */
    CALL,

    /** Email sent or received — expects an {@code EmailLog} sub-entity. */
    EMAIL,

    /** In-person or video meeting. */
    MEETING,

    /** Product or service demonstration. */
    DEMO,

    /** Internal free-text note logged by the commercial. */
    NOTE,

    /** Physical or virtual site visit. */
    VISIT,

    /** A completed {@code COMMERCIAL_ACTION} logged as an activity. */
    ACTION_DONE,

    /**
     * The initial contact form message submitted by the visitor.
     *
     * <p>System-generated automatically when a public lead is submitted.
     * {@code performedBy} is {@code null} for this type — the author is the visitor.</p>
     */
    CONTACT_FORM
}
