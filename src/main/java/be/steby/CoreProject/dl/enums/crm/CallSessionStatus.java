package be.steby.CoreProject.dl.enums.crm;

/**
 * Lifecycle status of a {@link be.steby.CoreProject.dl.entities.crm.CallSession}.
 *
 * <p>Tracks the technical state of a call from initiation to termination.
 * Terminal states ({@code ENDED}, {@code FAILED}, {@code MISSED}) trigger
 * the creation of a {@link be.steby.CoreProject.dl.entities.crm.CallLog}
 * and the associated {@link be.steby.CoreProject.dl.entities.crm.Interaction}.</p>
 *
 * <h3>State machine</h3>
 * <pre>
 *   INITIATED → RINGING → ACTIVE → ENDED
 *             ↘          ↘
 *             MISSED      FAILED
 *   INITIATED → FAILED   (provider error before ringing)
 * </pre>
 *
 * @see be.steby.CoreProject.dl.entities.crm.CallSession
 */
public enum CallSessionStatus {

    /** The call has been created and is being routed by the provider. */
    INITIATED,

    /** The remote party's device is ringing. */
    RINGING,

    /** The remote party answered — conversation is in progress. */
    ACTIVE,

    /** The call ended normally after being answered. */
    ENDED,

    /** The provider failed to place the call (network error, invalid number, etc.). */
    FAILED,

    /** The call rang but was not answered and did not go to voicemail. */
    MISSED
}
