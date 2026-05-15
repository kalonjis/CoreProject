package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.crm.CallSession;
import be.steby.CoreProject.dl.enums.crm.CallSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link CallSession} entity operations.
 *
 * <p>{@code CallSession} tracks the technical lifecycle of a call in progress.
 * Once a session reaches a terminal state, the service layer creates the
 * corresponding {@link be.steby.CoreProject.dl.entities.crm.CallLog} and
 * {@link be.steby.CoreProject.dl.entities.crm.Interaction}.</p>
 *
 * <h3>Query strategy</h3>
 * <ul>
 *   <li>Provider webhook correlation → {@link #findByExternalCallId}</li>
 *   <li>Timeline queries → derived methods ordered by {@code startedAt}</li>
 *   <li>Active session detection → {@link #findByStatusIn}</li>
 * </ul>
 */
@Repository
public interface CallSessionRepository extends JpaRepository<CallSession, Long> {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds a call session by its public UUID.
     *
     * @param publicId the public UUID
     * @return the session if found
     */
    Optional<CallSession> findByPublicId(String publicId);

    /**
     * Finds a call session by the provider's own call identifier.
     *
     * <p>Used to correlate incoming provider webhooks (Twilio, SIP) with
     * the correct session. Returns empty for {@code TEL_URI} sessions
     * which never set an external ID.</p>
     *
     * @param externalCallId the provider-specific call ID (e.g. Twilio Call SID)
     * @return the session if found
     */
    Optional<CallSession> findByExternalCallId(String externalCallId);

    // =========================================================================
    // Active session detection
    // =========================================================================

    /**
     * Finds all sessions currently in one of the given statuses.
     *
     * <p>Used to detect stale in-progress sessions on application startup
     * and to enforce the "one active call per commercial" rule.</p>
     *
     * @param statuses the statuses to include (typically non-terminal ones)
     * @return sessions matching any of the given statuses
     */
    List<CallSession> findByStatusIn(Collection<CallSessionStatus> statuses);

    /**
     * Checks whether a commercial currently has an active call session.
     *
     * <p>Used to prevent a commercial from initiating a second call
     * while one is already in progress.</p>
     *
     * @param performedById the internal ID of the commercial
     * @param statuses      the statuses considered "active"
     * @return {@code true} if at least one active session exists
     */
    boolean existsByPerformedByIdAndStatusIn(Long performedById, Collection<CallSessionStatus> statuses);

    // =========================================================================
    // Contact & lead history
    // =========================================================================

    /**
     * Finds all call sessions for a contact, most recent first.
     *
     * <p>Used to display call history in the contact detail view.</p>
     *
     * @param contactId the internal ID of the contact
     * @return sessions for that contact, most recent first
     */
    List<CallSession> findByContactIdOrderByStartedAtDesc(Long contactId);

    /**
     * Finds all call sessions for a lead, most recent first.
     *
     * <p>Used during lead qualification before conversion to a Contact.</p>
     *
     * @param leadId the internal ID of the lead
     * @return sessions for that lead, most recent first
     */
    List<CallSession> findByLeadIdOrderByStartedAtDesc(Long leadId);

    // =========================================================================
    // Reporting
    // =========================================================================

    /**
     * Finds all call sessions initiated by a commercial within a date range.
     *
     * <p>Used for individual call volume reporting.</p>
     *
     * @param performedById the internal ID of the commercial
     * @param from          start of the range (inclusive)
     * @param to            end of the range (inclusive)
     * @return sessions placed by that commercial in the range
     */
    List<CallSession> findByPerformedByIdAndStartedAtBetween(Long performedById, Instant from, Instant to);
}
