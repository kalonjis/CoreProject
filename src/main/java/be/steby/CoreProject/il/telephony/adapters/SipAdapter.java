package be.steby.CoreProject.il.telephony.adapters;

import be.steby.CoreProject.dl.entities.crm.CallSession;
import be.steby.CoreProject.dl.enums.crm.CallProvider;
import be.steby.CoreProject.dl.enums.crm.CallSessionStatus;
import be.steby.CoreProject.il.telephony.TelephonyPort;
import be.steby.CoreProject.il.telephony.model.InitiateCallCommand;
import be.steby.CoreProject.il.telephony.model.TerminateCallCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Telephony adapter for SIP/WebRTC calling via SIP.js + Asterisk.
 *
 * <h3>Architecture</h3>
 * <p>The actual SIP signaling is handled entirely by SIP.js on the frontend.
 * SIP.js connects to Asterisk via a secure WebSocket (WSS), registers as the
 * commercial's SIP extension, and sends the INVITE directly. This adapter's
 * responsibility is limited to creating and updating the {@link CallSession}
 * record in the CRM database.</p>
 *
 * <h3>Lifecycle events driven by SIP.js signals</h3>
 * <ol>
 *   <li>INVITE sent by SIP.js → {@link #initiate} called → session {@code INITIATED}</li>
 *   <li>180 Ringing → frontend updates UI only (no backend call)</li>
 *   <li>200 OK (answered) → {@code PATCH /{id}/answer} → session {@code ACTIVE}, {@code answeredAt} set</li>
 *   <li>BYE / 4xx / timeout → {@link #terminate} called → session terminal, {@code durationSeconds} computed</li>
 * </ol>
 *
 * <h3>Duration</h3>
 * <p>Unlike {@link TelUriAdapter} where duration is user-declared, this adapter
 * computes duration automatically from {@code session.answeredAt} to {@code Instant.now()}
 * when the command does not supply an explicit value.</p>
 *
 * @see TelephonyPort
 * @see TelUriAdapter
 */
@Slf4j
@Component
public class SipAdapter implements TelephonyPort {

    @Override
    public CallSession initiate(InitiateCallCommand command) {
        log.debug("SIP initiate — number: {}", command.phoneNumber());

        return CallSession.builder()
                .provider(CallProvider.SIP)
                .status(CallSessionStatus.INITIATED)
                .phoneNumber(command.phoneNumber())
                .startedAt(Instant.now())
                .contact(command.contact())
                .lead(command.lead())
                .performedBy(command.performedBy())
                .build();
    }

    @Override
    public void terminate(CallSession session, TerminateCallCommand command) {
        log.debug("SIP terminate — session: {}, status: {}", session.getPublicId(), command.status());

        Instant endedAt = Instant.now();
        session.setStatus(command.status());
        session.setEndedAt(endedAt);

        // Compute duration server-side from answeredAt when available (SIP precision).
        // Fall back to the value declared by the frontend for missed/failed calls.
        if (session.getAnsweredAt() != null && command.status() == CallSessionStatus.ENDED) {
            session.setDurationSeconds((int) (endedAt.getEpochSecond() - session.getAnsweredAt().getEpochSecond()));
        } else {
            session.setDurationSeconds(command.durationSeconds());
        }
    }

    @Override
    public CallProvider getProvider() {
        return CallProvider.SIP;
    }
}
