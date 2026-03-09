package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.BaseEntity;
import be.steby.CoreProject.dl.enums.crm.CallStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Stores structured phone call details for a {@link Interaction} of type {@code CALL}.
 *
 * <p>This entity complements the generic {@link Interaction} record with data
 * that is specific to phone calls: the number dialled, call duration, status,
 * and an optional recording URL.</p>
 *
 * <h3>Inheritance</h3>
 * <p>Extends {@link BaseEntity} to inherit:</p>
 * <ul>
 *   <li>{@code id} — internal Long PK, never exposed via API</li>
 *   <li>{@code publicId} — UUID exposed in all public endpoints</li>
 *   <li>{@code createdBy}, {@code createdAt}, {@code updatedBy}, {@code updatedAt} — audit fields</li>
 * </ul>
 *
 * <h3>Relationship</h3>
 * <p>Exactly one {@code CallLog} → one {@link Interaction} (owning side).
 * The {@code interaction} holds the inverse {@code @OneToOne(mappedBy = "interaction")}.</p>
 *
 * <h3>Recording URL</h3>
 * <p>{@code recordingUrl} is optional and only populated when a telephony
 * integration (e.g. Twilio) is configured. It points to a stored audio file
 * and should be access-controlled at the service layer.</p>
 *
 * <h3>Database indexes</h3>
 * <ul>
 *   <li>{@code idx_calllog_activity} — join from interaction to its CallLog</li>
 * </ul>
 *
 * @see Interaction
 * @see CallStatus
 */
@Entity
@Table(
    name = "crm_call_log",
    indexes = {
        @Index(name = "idx_calllog_interaction", columnList = "interaction_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class CallLog extends BaseEntity<Long> {

    // =========================================================================
    // Call details
    // =========================================================================

    /**
     * Phone number that was dialled or that called in.
     *
     * <p>Optional — may not be available for inbound calls from unknown numbers.
     * Stored as plain string to support all international formats.</p>
     *
     * <p>Example: "+32 475 12 34 56"</p>
     */
    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    /**
     * Duration of the call in seconds.
     *
     * <p>Optional — null if the call was not answered.
     * Displayed as mm:ss in the UI.</p>
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * Outcome status of the call attempt.
     *
     * <p>Required. Used to distinguish a productive call from a missed one
     * and to trigger automatic follow-up tasks.</p>
     *
     * @see CallStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    @ToString.Include
    private CallStatus status;

    /**
     * URL pointing to the call recording.
     *
     * <p>Optional. Only present when a telephony integration (e.g. Twilio,
     * RingCentral) is active. Access must be restricted to authorised users
     * at the service layer.</p>
     */
    @Column(name = "recording_url", length = 500)
    private String recordingUrl;

    // =========================================================================
    // Relationship
    // =========================================================================

    /**
     * The parent interaction this call log belongs to.
     *
     * <p>Required. Owning side of the {@code Interaction ↔ CallLog} relation.</p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interaction_id", nullable = false, unique = true)
    private Interaction interaction;
}