package be.steby.CoreProject.bll.domains.crm.call.services;

import be.steby.CoreProject.bll.domains.crm.call.exceptions.CallAlreadyActiveException;
import be.steby.CoreProject.bll.domains.crm.call.exceptions.CallSessionNotFoundException;
import be.steby.CoreProject.bll.domains.crm.call.exceptions.CallValidationException;
import be.steby.CoreProject.bll.domains.crm.call.models.InitiateCallRequest;
import be.steby.CoreProject.bll.domains.crm.call.models.TerminateCallRequest;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CallSession;

/**
 * Service for managing the lifecycle of CRM call sessions.
 *
 * <p>Orchestrates the telephony adapter (via {@code TelephonyAdapterResolver}),
 * persists {@link CallSession} state, and publishes domain events that drive
 * downstream interaction logging.</p>
 *
 * <h3>Call lifecycle</h3>
 * <pre>
 *   initiate() → CallSession(INITIATED) → [provider handles call]
 *   terminate() → CallSession(ENDED|MISSED|FAILED) → CallTerminatedEvent
 *                                                   → Interaction + CallLog created by listener
 * </pre>
 */
public interface CallService {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds a call session by its public UUID.
     *
     * @param publicId the public UUID of the session
     * @return the matching session
     * @throws CallSessionNotFoundException if not found
     */
    CallSession getByPublicId(String publicId);

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Initiates a call and creates the corresponding {@link CallSession}.
     *
     * <p>Business rules enforced:</p>
     * <ul>
     *   <li>At least one of {@code contactPublicId} or {@code leadPublicId} must be set</li>
     *   <li>The actor must not already have an active call session</li>
     * </ul>
     *
     * <p>Publishes a {@code CallInitiatedEvent} on success.</p>
     *
     * @param request the initiation parameters
     * @param actor   the commercial placing the call
     * @return the newly created and persisted call session
     * @throws CallValidationException    if validation fails
     * @throws CallAlreadyActiveException if the actor already has an active session
     */
    CallSession initiate(InitiateCallRequest request, User actor);

    /**
     * Terminates an active call session and triggers interaction logging.
     *
     * <p>Business rules enforced:</p>
     * <ul>
     *   <li>{@code status} must be a terminal state: {@code ENDED}, {@code MISSED}, or {@code FAILED}</li>
     *   <li>The session must not already be in a terminal state</li>
     * </ul>
     *
     * <p>Publishes a {@code CallTerminatedEvent} on success, which causes the
     * {@code CallTerminatedInteractionListener} to create the {@code Interaction}
     * and {@code CallLog} in the CRM timeline.</p>
     *
     * @param publicId the public UUID of the session to terminate
     * @param request  the termination parameters
     * @param actor    the commercial ending the call
     * @throws CallSessionNotFoundException if the session is not found
     * @throws CallValidationException      if the status is not terminal or session is already closed
     */
    void terminate(String publicId, TerminateCallRequest request, User actor);
}
