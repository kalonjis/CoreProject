package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.BaseEntity;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.crm.CommercialActionPriority;
import be.steby.CoreProject.dl.enums.crm.CommercialActionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Represents an action to be performed by a commercial in the CRM.
 *
 * <p>A {@code CommercialAction} is a concrete to-do item linked to a {@link Deal} and/or
 * a {@link Contact}. Tasks can be created manually by the commercial team
 * or generated automatically by the workflow engine (e.g. a follow-up CommercialAction
 * created when a call goes unanswered).</p>
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
 *   <li>Many {@code CommercialAction} → one {@link Deal} (optional)</li>
 *   <li>Many {@code CommercialAction} → one {@link Contact} (optional)</li>
 *   <li>Many {@code CommercialAction} → one {@link User} assignedTo — who must do it</li>
 *   <li>Many {@code CommercialAction} → one {@link User} createdBy — who created it (from {@code BaseEntity#createdBy} string, not a FK)</li>
 * </ul>
 * <p>At least one of {@code deal} or {@code contact} must be set.
 * Enforced at the service layer.</p>
 *
 * <h3>Completion</h3>
 * <p>When a CommercialAction is completed, the service sets {@code status = DONE}
 * and records {@code completedAt}. Optionally, a linked {@link Interaction}
 * of type {@code ACTION_DONE} can be created to keep the deal timeline complete.</p>
 *
 * <h3>Database indexes</h3>
 * <ul>
 *   <li>{@code idx_CommercialAction_public_id}  — fast lookup by UUID (API)</li>
 *   <li>{@code idx_CommercialAction_assigned}   — all CommercialAction for a given commercial (my tasks view)</li>
 *   <li>{@code idx_CommercialAction_deal}       — all CommercialAction linked to a deal</li>
 *   <li>{@code idx_CommercialAction_contact}    — all CommercialAction linked to a contact</li>
 *   <li>{@code idx_CommercialAction_status}     — filter pending / done / cancelled</li>
 *   <li>{@code idx_CommercialAction_due}        — overdue CommercialAction detection</li>
 * </ul>
 *
 * @see CommercialActionStatus
 * @see CommercialActionPriority
 * @see Interaction
 */
@Entity
@Table(
    indexes = {
        @Index(name = "idx_CommercialAction_public_id", columnList = "public_id"),
        @Index(name = "idx_CommercialAction_assigned",  columnList = "assigned_to_id"),
        @Index(name = "idx_CommercialAction_deal",      columnList = "deal_id"),
        @Index(name = "idx_CommercialAction_contact",   columnList = "contact_id"),
        @Index(name = "idx_CommercialAction_lead",      columnList = "lead_id"),
        @Index(name = "idx_CommercialAction_status",    columnList = "status"),
        @Index(name = "idx_CommercialAction_due",       columnList = "due_date"),
        @Index(name = "idx_CommercialAction_reminder",  columnList = "reminder_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class CommercialAction extends BaseEntity<Long> {

    // =========================================================================
    // Identity
    // =========================================================================

    /**
     * Short title describing what needs to be done.
     *
     * <p>Required. Shown in CommercialAction lists, deal sidebars, and notifications.</p>
     *
     * <p>Example: "Call back Thomas — quote follow-up",
     * "Send updated contract to ACME SA"</p>
     */
    @Column(name = "title", nullable = false, length = 200)
    @ToString.Include
    private String title;

    /**
     * Optional longer description providing context or instructions.
     *
     * <p>Shown in the CommercialAction detail view. Useful for auto-generated tasks
     * where the workflow engine adds context about why the CommercialAction was created.</p>
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // =========================================================================
    // Classification
    // =========================================================================

    /**
     * Priority level of the CommercialAction.
     *
     * <p>Required. Defaults to {@link CommercialActionPriority#MEDIUM}.
     * Used to sort the commercial's CommercialAction list and to highlight urgent items.</p>
     *
     * @see CommercialActionPriority
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    @Builder.Default
    private CommercialActionPriority priority = CommercialActionPriority.MEDIUM;

    /**
     * Current status of the CommercialAction.
     *
     * <p>Required. Defaults to {@link CommercialActionStatus#PENDING} on creation.
     * Transitions are managed by the service layer.</p>
     *
     * @see CommercialActionStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    @Builder.Default
    private CommercialActionStatus status = CommercialActionStatus.PENDING;

    // =========================================================================
    // Timing
    // =========================================================================

    /**
     * Deadline by which the CommercialAction must be completed.
     *
     * <p>Optional. Used to detect overdue tasks and trigger reminder
     * notifications. Stored as {@link Instant} to support time-based deadlines
     * (e.g. "call before 10:00 tomorrow").</p>
     */
    @Column(name = "due_date")
    private Instant dueDate;

    /**
     * Timestamp when the CommercialAction was marked as done.
     *
     * <p>Set by the service layer when {@code status} transitions to {@code DONE}.
     * Null if the CommercialAction is still pending or was cancelled.</p>
     */
    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Timestamp at which the reminder should be sent.
     *
     * <p>Optional. When set, the {@code CommercialActionReminderScheduler} will
     * notify the assigned commercial via their preferred channels (IN_APP / EMAIL)
     * once this instant is reached. Null means no reminder is scheduled.</p>
     */
    @Column(name = "reminder_at")
    private Instant reminderAt;

    /**
     * Timestamp when the reminder was actually dispatched.
     *
     * <p>Set by the scheduler after successful dispatch. Null until the reminder
     * fires. Used for idempotence — the scheduler will not re-send if non-null.</p>
     */
    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    // =========================================================================
    // Relationships
    // =========================================================================

    /**
     * The commercial responsible for completing this CommercialAction.
     *
     * <p>Required. Shown in the "My Tasks" dashboard view and used
     * for notification delivery.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id", nullable = false)
    private User assignedTo;

    /**
     * Deal this CommercialAction is linked to.
     *
     * <p>Optional — at least one of {@code deal} or {@code contact} must be set.
     * Loaded lazily.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id")
    private Deal deal;

    /**
     * Contact this CommercialAction is linked to.
     *
     * <p>Optional — at least one of {@code lead}, {@code deal}, or {@code contact} must be set.
     * Loaded lazily.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    /**
     * Lead this CommercialAction is linked to.
     *
     * <p>Optional — set when creating a task during lead qualification,
     * before conversion to a Contact.
     * At least one of {@code lead}, {@code deal}, or {@code contact} must be set.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    // =========================================================================
    // Utility methods
    // =========================================================================

    /**
     * Returns {@code true} if the CommercialAction has passed its deadline without being completed.
     *
     * @return true if pending and {@code dueDate} is in the past
     */
    public boolean isOverdue() {
        return this.status == CommercialActionStatus.PENDING
            && this.dueDate != null
            && this.dueDate.isBefore(Instant.now());
    }
}