package be.steby.CoreProject.il.telephony.model;

import be.steby.CoreProject.dl.enums.crm.CallSessionStatus;

/**
 * Command carrying the parameters required to terminate a call session.
 *
 * <p>For {@link be.steby.CoreProject.dl.enums.crm.CallProvider#TEL_URI}, both fields
 * are provided by the user through the post-call confirmation modal.
 * For provider-backed adapters (Twilio, SIP), they are sourced from the
 * provider webhook or SIP BYE event.</p>
 *
 * @param status          the terminal status of the call (must be a terminal state:
 *                        {@code ENDED}, {@code FAILED}, or {@code MISSED})
 * @param durationSeconds the connected call duration in seconds;
 *                        {@code null} for missed or failed calls
 */
public record TerminateCallCommand(
        CallSessionStatus status,
        Integer durationSeconds
) {}
