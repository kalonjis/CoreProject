package be.steby.CoreProject.il.telephony;

import be.steby.CoreProject.dl.entities.crm.CallSession;
import be.steby.CoreProject.dl.enums.crm.CallProvider;
import be.steby.CoreProject.il.telephony.model.InitiateCallCommand;
import be.steby.CoreProject.il.telephony.model.TerminateCallCommand;

/**
 * Strategy contract for telephony provider adapters.
 *
 * <p>Each implementation wraps a specific telephony provider
 * ({@link CallProvider#TEL_URI}, {@link CallProvider#TWILIO}, {@link CallProvider#SIP})
 * and exposes a uniform interface to the {@code bll} service layer.
 * The active adapter is resolved at runtime by {@link TelephonyAdapterResolver}
 * based on the tenant's {@link be.steby.CoreProject.dl.entities.crm.TelephonyConfig}.</p>
 *
 * <h3>Persistence contract</h3>
 * <p>Adapters operate in-memory only — they do not persist the {@link CallSession}.
 * The caller (service layer) is responsible for saving and updating the entity.</p>
 *
 * @see TelephonyAdapterResolver
 * @see be.steby.CoreProject.il.telephony.adapters.TelUriAdapter
 * @see be.steby.CoreProject.il.telephony.adapters.TwilioAdapter
 * @see be.steby.CoreProject.il.telephony.adapters.SipAdapter
 */
public interface TelephonyPort {

    /**
     * Initiates a call and returns an unpersisted {@link CallSession}.
     *
     * <p>For provider-backed adapters (Twilio, SIP), this triggers the actual
     * API or SIP INVITE. For {@link CallProvider#TEL_URI}, it pre-fills the session
     * without any external call — the OS handles dialling via a {@code tel:} URI
     * triggered on the frontend.</p>
     *
     * @param command the initiation parameters (phone number, contact or lead, commercial)
     * @return an in-memory {@link CallSession} ready to be persisted by the service layer
     */
    CallSession initiate(InitiateCallCommand command);

    /**
     * Terminates a call session and updates its fields in-memory.
     *
     * <p>Sets the terminal status, end time, and duration on the provided session.
     * The caller must persist the updated entity after this method returns.</p>
     *
     * @param session the active session to terminate
     * @param command the termination parameters (status, duration)
     */
    void terminate(CallSession session, TerminateCallCommand command);

    /**
     * Returns the telephony provider this adapter handles.
     *
     * <p>Used by {@link TelephonyAdapterResolver} to select the correct adapter
     * at runtime.</p>
     *
     * @return the provider constant this adapter is registered for
     */
    CallProvider getProvider();
}
