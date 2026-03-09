package be.steby.CoreProject.dl.enums.crm;

import be.steby.CoreProject.dl.entities.crm.Interaction;

/**
 * Outcome of an {@link Interaction}.
 *
 * <p>Gives a quick visual indicator in the timeline without
 * requiring the commercial to read the full notes.</p>
 */
public enum InteractionOutcome {

    /** Interaction went well — contact is engaged or moving forward. */
    POSITIVE,

    /** Interaction was neutral — no clear progress or setback. */
    NEUTRAL,

    /** Interaction revealed resistance or a problem. */
    NEGATIVE,

    /** Contact did not answer (relevant for calls). */
    NO_ANSWER
}