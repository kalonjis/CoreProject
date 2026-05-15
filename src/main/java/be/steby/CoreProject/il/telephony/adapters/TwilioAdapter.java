package be.steby.CoreProject.il.telephony.adapters;

import be.steby.CoreProject.dl.entities.crm.CallSession;
import be.steby.CoreProject.dl.enums.crm.CallProvider;
import be.steby.CoreProject.il.telephony.TelephonyPort;
import be.steby.CoreProject.il.telephony.model.InitiateCallCommand;
import be.steby.CoreProject.il.telephony.model.TerminateCallCommand;
import org.springframework.stereotype.Component;

/**
 * Telephony adapter for the Twilio Voice platform.
 *
 * <p>Places calls via the Twilio Voice REST API and receives lifecycle events
 * through Twilio status-callback webhooks. Requires a Twilio Account SID,
 * Auth Token, and a provisioned Twilio phone number stored in
 * {@link be.steby.CoreProject.dl.entities.crm.TelephonyConfig#getEncryptedCredentials()}.</p>
 *
 * <p><strong>Not yet implemented.</strong> Activate once the Twilio SDK dependency
 * ({@code com.twilio.sdk:twilio}) is added to the project.</p>
 *
 * @see TelephonyPort
 */
@Component
public class TwilioAdapter implements TelephonyPort {

    @Override
    public CallSession initiate(InitiateCallCommand command) {
        throw new UnsupportedOperationException("Twilio adapter not yet implemented");
    }

    @Override
    public void terminate(CallSession session, TerminateCallCommand command) {
        throw new UnsupportedOperationException("Twilio adapter not yet implemented");
    }

    @Override
    public CallProvider getProvider() {
        return CallProvider.TWILIO;
    }
}
