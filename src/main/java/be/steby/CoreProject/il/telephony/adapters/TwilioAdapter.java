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
 * Telephony adapter for the Twilio Voice platform (Voice SDK + TwiML App).
 *
 * <h3>Architecture</h3>
 * <p>The actual call audio is handled by the Twilio Voice SDK ({@code @twilio/voice-sdk})
 * running in the commercial's browser. The browser connects to Twilio's WebRTC edge
 * network, which calls the customer's phone via the configured TwiML Application.</p>
 *
 * <h3>Lifecycle driven by Twilio webhooks and frontend events</h3>
 * <ol>
 *   <li>{@code device.connect()} in browser → Twilio POSTs to TwiML App URL
 *       → backend registers {@code externalCallId} and returns Dial TwiML</li>
 *   <li>Customer's phone rings → webhook {@code ringing} → session {@code RINGING}</li>
 *   <li>Customer answers → webhook {@code in-progress} → session {@code ACTIVE}, {@code answeredAt} set</li>
 *   <li>Either party hangs up → frontend disconnect event → PATCH /terminate → session terminal</li>
 *   <li>Twilio confirms via webhook {@code completed} — if frontend already terminated, ignore</li>
 * </ol>
 *
 * <h3>Duration</h3>
 * <p>Uses the caller-supplied {@code durationSeconds} when present (e.g. from Twilio's
 * {@code CallDuration} webhook param). Otherwise computes server-side from {@code answeredAt},
 * consistent with {@link SipAdapter}.</p>
 *
 * @see TelephonyPort
 * @see SipAdapter
 */
@Slf4j
@Component
public class TwilioAdapter implements TelephonyPort {

    @Override
    public CallSession initiate(InitiateCallCommand command) {
        log.debug("Twilio initiate — number: {}", command.phoneNumber());

        // No Twilio REST API call here. The browser's Twilio Voice SDK initiates the
        // WebRTC connection directly. The externalCallId (Twilio CallSid) is registered
        // later when Twilio POSTs to the TwiML App URL (/api/crm/telephony/twilio/twiml).
        return CallSession.builder()
                .provider(CallProvider.TWILIO)
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
        log.debug("Twilio terminate — session: {}, status: {}", session.getPublicId(), command.status());

        Instant endedAt = Instant.now();
        session.setStatus(command.status());
        session.setEndedAt(endedAt);

        // Prefer caller-supplied duration (e.g. from Twilio webhook's CallDuration param).
        // Fall back to server-side computation from answeredAt for consistency with SipAdapter.
        if (command.durationSeconds() != null) {
            session.setDurationSeconds(command.durationSeconds());
        } else if (session.getAnsweredAt() != null && command.status() == CallSessionStatus.ENDED) {
            session.setDurationSeconds((int) (endedAt.getEpochSecond() - session.getAnsweredAt().getEpochSecond()));
        }
    }

    @Override
    public CallProvider getProvider() {
        return CallProvider.TWILIO;
    }
}
