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
 * Universal fallback telephony adapter using the OS {@code tel:} URI scheme.
 *
 * <p>This adapter requires no credentials and no external API. The actual call is
 * placed by the operating system's default dialler (Windows Phone Link, Skype, etc.)
 * triggered via a {@code tel:} URI on the frontend. The CRM tracks the session
 * using timestamps submitted by the user through the post-call confirmation modal.</p>
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Frontend clicks the phone number → {@code tel:} URI opens the OS dialler</li>
 *   <li>{@link #initiate} is called simultaneously → session created with {@code INITIATED}</li>
 *   <li>User confirms the call outcome in the modal → {@link #terminate} is called</li>
 *   <li>Service layer persists the session and creates the {@link be.steby.CoreProject.dl.entities.crm.Interaction}</li>
 * </ol>
 *
 * <h3>Limitations</h3>
 * <p>Duration is user-reported and therefore approximate.
 * No automatic webhook or SIP event is available with this adapter.</p>
 *
 * @see TelephonyPort
 */
@Slf4j
@Component
public class TelUriAdapter implements TelephonyPort {

    @Override
    public CallSession initiate(InitiateCallCommand command) {
        log.debug("TEL_URI initiate — number: {}", command.phoneNumber());

        return CallSession.builder()
                .provider(CallProvider.TEL_URI)
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
        log.debug("TEL_URI terminate — session: {}, status: {}, duration: {}s",
                session.getPublicId(), command.status(), command.durationSeconds());

        session.setStatus(command.status());
        session.setEndedAt(Instant.now());
        session.setDurationSeconds(command.durationSeconds());
    }

    @Override
    public CallProvider getProvider() {
        return CallProvider.TEL_URI;
    }
}
