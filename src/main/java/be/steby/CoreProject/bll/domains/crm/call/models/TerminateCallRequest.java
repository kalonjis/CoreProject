package be.steby.CoreProject.bll.domains.crm.call.models;

import be.steby.CoreProject.dl.enums.crm.CallSessionStatus;

/**
 * BLL request model for terminating an active call session.
 *
 * <p>{@code status} must be a terminal value: {@code ENDED}, {@code MISSED}, or {@code FAILED}.
 * This constraint is enforced by {@code CallServiceImpl}.</p>
 *
 * @param status          the terminal status of the call
 * @param durationSeconds connected duration in seconds; {@code null} for missed or failed calls
 */
public record TerminateCallRequest(
        CallSessionStatus status,
        Integer durationSeconds
) {}
