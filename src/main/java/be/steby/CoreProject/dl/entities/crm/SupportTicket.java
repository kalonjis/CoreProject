package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.BaseEntity;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.crm.SupportTicketStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a support request submitted by a {@link Contact} to the team.
 *
 * <p>A {@code SupportTicket} is created when a contact reports an issue,
 * asks a question, or requests assistance. It follows a simple lifecycle
 * from {@code OPEN} through to {@code CLOSED}.</p>
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
 *   <li>Many {@code SupportTicket} → one {@link Contact} (submitted by)</li>
 *   <li>Many {@code SupportTicket} → one {@link User} (assigned to, optional)</li>
 * </ul>
 *
 * <h3>Lifecycle</h3>
 * <pre>
 * OPEN ──► IN_PROGRESS ──► RESOLVED ──► CLOSED
 *  └──────────────────────────────────► CLOSED (direct close)
 * </pre>
 *
 * <h3>Database indexes</h3>
 * <ul>
 *   <li>{@code idx_ticket_public_id}  — fast lookup by UUID (API)</li>
 *   <li>{@code idx_ticket_contact}    — all tickets for a contact</li>
 *   <li>{@code idx_ticket_assigned}   — all tickets assigned to a team member</li>
 *   <li>{@code idx_ticket_status}     — filter by lifecycle status</li>
 * </ul>
 *
 * @see Contact
 * @see SupportTicketStatus
 */
@Entity
@Table(
    name = "crm_support_ticket",
    indexes = {
        @Index(name = "idx_ticket_public_id", columnList = "public_id"),
        @Index(name = "idx_ticket_contact",   columnList = "submitted_by_id"),
        @Index(name = "idx_ticket_assigned",  columnList = "assigned_to_id"),
        @Index(name = "idx_ticket_status",    columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class SupportTicket extends BaseEntity<Long> {

    // =========================================================================
    // Content
    // =========================================================================

    /**
     * Short subject line of the ticket.
     *
     * <p>Required. Shown in the ticket list and as the heading of the detail view.</p>
     *
     * <p>Example: "Unable to access my account", "Invoice issue — July 2025"</p>
     */
    @Column(name = "subject", nullable = false, length = 255)
    @ToString.Include
    private String subject;

    /**
     * Detailed description of the issue or request.
     *
     * <p>Optional at creation but strongly recommended.
     * Written by the contact or a team member on their behalf.</p>
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // =========================================================================
    // Status
    // =========================================================================

    /**
     * Current lifecycle status of the ticket.
     *
     * <p>Required. Defaults to {@link SupportTicketStatus#OPEN} on creation.
     * Transitions are driven by team actions via the service layer.</p>
     *
     * @see SupportTicketStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    @Builder.Default
    @ToString.Include
    private SupportTicketStatus status = SupportTicketStatus.OPEN;

    // =========================================================================
    // Relationships
    // =========================================================================

    /**
     * Contact who submitted this ticket.
     *
     * <p>Required. Identifies who is reporting the issue.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id", nullable = false)
    private Contact submittedBy;

    /**
     * Team member (User) currently assigned to handle this ticket.
     *
     * <p>Optional — tickets can be unassigned until picked up.
     * Loaded lazily — not needed in list views.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    // =========================================================================
    // Utility methods
    // =========================================================================

    /**
     * Returns {@code true} if this ticket is in a terminal state.
     *
     * @return true if {@code status} is {@code CLOSED}
     */
    public boolean isClosed() {
        return this.status == SupportTicketStatus.CLOSED;
    }
}
