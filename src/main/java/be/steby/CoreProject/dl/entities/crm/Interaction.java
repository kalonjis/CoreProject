package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.BaseEntity;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.crm.InteractionDirection;
import be.steby.CoreProject.dl.enums.crm.InteractionOutcome;
import be.steby.CoreProject.dl.enums.crm.InteractionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Records a single interaction or touchpoint in the CRM timeline.
 *
 * <p>An {@code Interaction} is created each time a commercial action occurs
 * around a {@link Deal} or a {@link Contact}: a phone call, an email sent,
 * a meeting, an internal note, etc. Together they form the full audit trail
 * visible in the deal and contact timeline views.</p>
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
 *   <li>Many {@code Interaction} → one {@link Deal} (optional — interaction can be contact-only)</li>
 *   <li>Many {@code Interaction} → one {@link Contact} (optional — interaction can be deal-only)</li>
 *   <li>Many {@code Interaction} → one {@link User} (the commercial who performed the action)</li>
 * </ul>
 * <p>At least one of {@code deal} or {@code contact} must be set.
 * This constraint is enforced at the service layer.</p>
 *
 * <h3>Type-specific detail tables</h3>
 * <p>For structured data specific to a type, dedicated sub-entities exist:</p>
 * <ul>
 *   <li>{@link CallLog} — phone call details (duration, status, recording URL)</li>
 *   <li>{@link EmailLog} — email details (external message ID, open/click tracking)</li>
 * </ul>
 *
 * <h3>Database indexes</h3>
 * <ul>
 *   <li>{@code idx_activity_public_id} — fast lookup by UUID (API)</li>
 *   <li>{@code idx_activity_deal}      — all activities for a deal (timeline)</li>
 *   <li>{@code idx_activity_contact}   — all activities for a contact (timeline)</li>
 *   <li>{@code idx_activity_user}      — all activities performed by a commercial</li>
 *   <li>{@code idx_activity_occurred}  — chronological ordering</li>
 * </ul>
 *
 * @see InteractionType
 * @see InteractionDirection
 * @see InteractionOutcome
 * @see CallLog
 * @see EmailLog
 */
@Entity
@Table(
    name = "crm_interaction",
    indexes = {
        @Index(name = "idx_interaction_public_id", columnList = "public_id"),
        @Index(name = "idx_interaction_deal",      columnList = "deal_id"),
        @Index(name = "idx_interaction_contact",   columnList = "contact_id"),
        @Index(name = "idx_interaction_user",      columnList = "performed_by_id"),
        @Index(name = "idx_interaction_occurred",  columnList = "occurred_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class Interaction extends BaseEntity<Long> {

    // =========================================================================
    // Classification
    // =========================================================================

    /**
     * Type of action that took place.
     *
     * <p>Required. Determines the icon shown in the timeline and whether
     * a {@link CallLog} or {@link EmailLog} detail record is expected.</p>
     *
     * @see InteractionType
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    @ToString.Include
    private InteractionType type;

    /**
     * Direction of the interaction relative to the company.
     *
     * <p>Optional — not applicable for internal notes or meetings.
     * {@code OUTBOUND} means the commercial initiated the action,
     * {@code INBOUND} means the contact initiated it.</p>
     *
     * @see InteractionDirection
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 10)
    private InteractionDirection direction;

    /**
     * Short subject or title of the interaction.
     *
     * <p>Required. Shown as the heading in the timeline card.</p>
     *
     * <p>Example: "Discovery call", "Quote follow-up", "Contract sent"</p>
     */
    @Column(name = "subject", nullable = false, length = 200)
    @ToString.Include
    private String subject;

    /**
     * Free-text summary of what happened during the interaction.
     *
     * <p>Optional. Written by the commercial after the interaction.
     * Shown in the expanded timeline card.</p>
     *
     * <p>Example: "Client confirmed interest in 2x/week cleaning.
     * Budget around 1000€/month. Follow up with quote by Friday."</p>
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Outcome of the interaction.
     *
     * <p>Optional. Gives a quick visual indicator in the timeline
     * without reading the full notes.</p>
     *
     * @see InteractionOutcome
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 10)
    private InteractionOutcome outcome;

    /**
     * Duration of the interaction in minutes.
     *
     * <p>Optional. Relevant for calls and meetings.
     * Null for emails, notes, and other non-timed activities.</p>
     */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    // =========================================================================
    // Timing
    // =========================================================================

    /**
     * When the interaction actually took place.
     *
     * <p>Required. May differ from {@code createdAt} if the commercial
     * logs the interaction after the fact (e.g., logging a call that happened yesterday).</p>
     */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    // =========================================================================
    // Relationships
    // =========================================================================

    /**
     * Deal this interaction is linked to.
     *
     * <p>Optional but at least one of {@code deal} or {@code contact} must be set.
     * Loaded lazily — not needed when browsing the contact timeline.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id")
    private Deal deal;

    /**
     * Contact this interaction is linked to.
     *
     * <p>Optional but at least one of {@code deal} or {@code contact} must be set.
     * Loaded lazily — not needed when browsing the deal timeline.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    /**
     * The commercial (User) who performed this interaction.
     *
     * <p>Required. Used for performance reporting and team interaction feeds.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id", nullable = false)
    private User performedBy;

    // =========================================================================
    // Detail sub-entities
    // =========================================================================

    /**
     * Structured call details, present only when {@code type == CALL}.
     *
     * <p>Null for all other interaction types.</p>
     */
    @OneToOne(mappedBy = "interaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private CallLog callLog;

    /**
     * Structured email details, present only when {@code type == EMAIL}.
     *
     * <p>Null for all other interaction types.</p>
     */
    @OneToOne(mappedBy = "interaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private EmailLog emailLog;
}