package be.steby.CoreProject.dl.enums.crm;

/**
 * Status of a phone call recorded in a {@link be.steby.CoreProject.dl.entities.crm.CallLog}.
 *
 * <p>Used to distinguish productive calls from missed attempts,
 * and to trigger automatic follow-up tasks in the workflow engine.</p>
 *
 * <h3>Workflow implications</h3>
 * <ul>
 *   <li>{@code NO_ANSWER} / {@code VOICEMAIL} → auto-create a follow-up task</li>
 *   <li>{@code ANSWERED} → commercial fills in notes and outcome manually</li>
 * </ul>
 */
public enum CallStatus {

    /** The contact picked up and a conversation took place. */
    ANSWERED,

    /** The call went to voicemail — message may or may not have been left. */
    VOICEMAIL,

    /** The call rang but was not answered and did not go to voicemail. */
    NO_ANSWER
}