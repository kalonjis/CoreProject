package be.steby.CoreProject.il.telephony.adapters;

import be.steby.CoreProject.dl.entities.crm.CallSession;
import be.steby.CoreProject.dl.enums.crm.CallProvider;
import be.steby.CoreProject.il.telephony.TelephonyPort;
import be.steby.CoreProject.il.telephony.model.InitiateCallCommand;
import be.steby.CoreProject.il.telephony.model.TerminateCallCommand;
import org.springframework.stereotype.Component;

/**
 * Telephony adapter for direct SIP/WebRTC calling via SIP.js.
 *
 * <p>Connects to the tenant's own SIP server using credentials stored in
 * {@link be.steby.CoreProject.dl.entities.crm.TelephonyConfig#getEncryptedCredentials()}.
 * Suitable for tenants whose VoIP subscription exposes SIP credentials
 * (server URI, username, password).</p>
 *
 * <p><strong>Not yet implemented.</strong> Requires coordination with the
 * frontend SIP.js integration and a backend SIP event bridge.</p>
 *
 * @see TelephonyPort
 */
@Component
public class SipAdapter implements TelephonyPort {

    @Override
    public CallSession initiate(InitiateCallCommand command) {
        throw new UnsupportedOperationException("SIP adapter not yet implemented");
    }

    @Override
    public void terminate(CallSession session, TerminateCallCommand command) {
        throw new UnsupportedOperationException("SIP adapter not yet implemented");
    }

    @Override
    public CallProvider getProvider() {
        return CallProvider.SIP;
    }
}
