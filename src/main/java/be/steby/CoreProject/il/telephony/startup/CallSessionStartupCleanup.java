package be.steby.CoreProject.il.telephony.startup;

import be.steby.CoreProject.dal.repositories.crm.CallSessionRepository;
import be.steby.CoreProject.dl.enums.crm.CallSessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

/**
 * Marks stale call sessions as FAILED on application startup.
 *
 * Any session left in INITIATED / RINGING / ACTIVE state when the server
 * stopped will never receive a termination signal, so we fail them eagerly.
 * No domain events are published — these sessions never reached a real
 * terminal state and should not generate Interactions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CallSessionStartupCleanup {

    private static final Set<CallSessionStatus> ACTIVE_STATUSES = Set.of(
            CallSessionStatus.INITIATED,
            CallSessionStatus.RINGING,
            CallSessionStatus.ACTIVE
    );

    private final CallSessionRepository callSessionRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void failStaleSessions() {
        int count = callSessionRepository.failAllActiveSessionsAt(ACTIVE_STATUSES, Instant.now());
        if (count > 0) {
            log.info("Startup cleanup — marked {} stale call session(s) as FAILED", count);
        }
    }
}
