package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.BaseEntity;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.crm.CallProvider;
import be.steby.CoreProject.dl.enums.crm.CallSessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Tracks the technical lifecycle of an outbound or inbound phone call.
 *
 * <p>{@code CallSession} exists for the entire duration of a call, from the moment
 * the CRM initiates it until the provider reports it as terminated. Once the session
 * reaches a terminal state ({@link CallSessionStatus#ENDED}, {@link CallSessionStatus#FAILED},
 * or {@link CallSessionStatus#MISSED}), the service layer creates a {@link CallLog}
 * and its parent {@link Interaction} to record the business fact in the CRM timeline.</p>
 *
 * <h3>Separation of concerns</h3>
 * <ul>
 *   <li>{@code CallSession} — technical, ephemeral, provider-aware (this entity)</li>
 *   <li>{@link CallLog} — business fact, post-call, provider-agnostic</li>
 * </ul>
 *
 * <h3>Inheritance</h3>
 * <p>Extends {@link BaseEntity} to inherit:</p>
 * <ul>
 *   <li>{@code id} — internal Long PK, never exposed via API</li>
 *   <li>{@code publicId} — UUID exposed in all public endpoints</li>
 *   <li>{@code createdBy}, {@code createdAt}, {@code updatedBy}, {@code updatedAt} — audit fields</li>
 * </ul>
 *
 * <h3>Relationships</h3>
 * <ul>
 *   <li>Many {@code CallSession} → one {@link Contact} (optional)</li>
 *   <li>Many {@code CallSession} → one {@link Lead} (optional)</li>
 *   <li>Many {@code CallSession} → one {@link User} (the commercial who placed the call)</li>
 * </ul>
 * <p>At least one of {@code contact} or {@code lead} must be set.
 * This constraint is enforced at the service layer.</p>
 *
 * <h3>Database indexes</h3>
 * <ul>
 *   <li>{@code idx_callsession_public_id} — fast UUID lookup (API)</li>
 *   <li>{@code idx_callsession_contact}   — all sessions for a contact</li>
 *   <li>{@code idx_callsession_lead}      — all sessions for a lead</li>
 *   <li>{@code idx_callsession_user}      — all sessions placed by a commercial</li>
 *   <li>{@code idx_callsession_status}    — filter active sessions</li>
 * </ul>
 *
 * @see CallSessionStatus
 * @see CallProvider
 * @see CallLog
 * @see Interaction
 */
@Entity
@Table(
    name = "crm_call_session",
    indexes = {
        @Index(name = "idx_callsession_public_id", columnList = "public_id"),
        @Index(name = "idx_callsession_contact",   columnList = "contact_id"),
        @Index(name = "idx_callsession_lead",      columnList = "lead_id"),
        @Index(name = "idx_callsession_user",      columnList = "performed_by_id"),
        @Index(name = "idx_callsession_status",    columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class CallSession extends BaseEntity<Long> {

    // =========================================================================
    // Provider & identification
    // =========================================================================

    /**
     * Telephony adapter that handled this call.
     *
     * <p>Required. Determines how lifecycle events are reported
     * (webhook for {@code TWILIO}, SIP events for {@code SIP},
     * manual confirmation for {@code TEL_URI}).</p>
     *
     * @see CallProvider
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 15)
    @ToString.Include
    private CallProvider provider;

    /**
     * Provider-specific call identifier.
     *
     * <p>Optional — null for {@link CallProvider#TEL_URI} which delegates
     * entirely to the OS. Populated by the provider on call initiation
     * (e.g., Twilio Call SID, SIP Call-ID header).</p>
     *
     * <p>Used to correlate incoming provider webhooks with this session.</p>
     */
    @Column(name = "external_call_id", length = 100)
    private String externalCallId;

    // =========================================================================
    // Call details
    // =========================================================================

    /**
     * Phone number that was dialled.
     *
     * <p>Required. Stored in the format provided by the user or contact record.
     * E.164 format is preferred (e.g., "+32475123456") but not enforced here —
     * normalisation is the responsibility of the service layer.</p>
     */
    @Column(name = "phone_number", nullable = false, length = 30)
    @ToString.Include
    private String phoneNumber;

    /**
     * Current lifecycle status of the call.
     *
     * <p>Required. Updated by the service as provider events arrive.
     * Terminal states trigger {@link Interaction} and {@link CallLog} creation.</p>
     *
     * @see CallSessionStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    @ToString.Include
    private CallSessionStatus status;

    // =========================================================================
    // Timing
    // =========================================================================

    /**
     * When the call was initiated by the CRM.
     *
     * <p>Required. Set at session creation, independently of {@code createdAt}
     * which reflects the DB insert time.</p>
     */
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    /**
     * When the remote party answered the call.
     *
     * <p>Optional — null until the call transitions to {@link CallSessionStatus#ACTIVE}.
     * Never set for missed or failed calls.</p>
     */
    @Column(name = "answered_at")
    private Instant answeredAt;

    /**
     * When the call was terminated.
     *
     * <p>Optional — null while the call is in progress.
     * Set when the session reaches any terminal status.</p>
     */
    @Column(name = "ended_at")
    private Instant endedAt;

    /**
     * Duration of the connected call in seconds.
     *
     * <p>Optional — null for missed or failed calls, and while the call is active.
     * Computed from {@code answeredAt} to {@code endedAt}, or provided directly
     * by the telephony provider (e.g., Twilio webhook payload).</p>
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    // =========================================================================
    // Relationships
    // =========================================================================

    /**
     * Contact targeted by this call.
     *
     * <p>Optional — at least one of {@code contact} or {@code lead} must be set.
     * Loaded lazily.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    /**
     * Lead targeted by this call.
     *
     * <p>Optional — set when calling a lead that has not yet been converted
     * to a {@link Contact}. At least one of {@code contact} or {@code lead} must be set.
     * Loaded lazily.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    /**
     * The commercial (User) who initiated this call.
     *
     * <p>Required. Used to attribute the call in reporting and to pre-fill
     * the {@link Interaction} created at the end of the session.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id", nullable = false)
    private User performedBy;
}
