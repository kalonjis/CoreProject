package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.BaseEntity;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.crm.DealStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Represents a commercial opportunity in the CRM pipeline.
 *
 * <p>A {@code Deal} is the central entity of the CRM. It tracks a sales
 * opportunity from its creation (converted from a {@link Contact}) through
 * all pipeline stages until it is won or lost.</p>
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
 *   <li>Many {@code Deal} → one {@link Pipeline}</li>
 *   <li>Many {@code Deal} → one {@link PipelineStep} (current stage)</li>
 *   <li>Many {@code Deal} → one {@link Contact} (primary contact)</li>
 *   <li>Many {@code Deal} → one {@link Organisation} (optional)</li>
 *   <li>Many {@code Deal} → one {@link User} (assigned commercial)</li>
 * </ul>
 *
 * <h3>Lifecycle</h3>
 * <pre>
 * OPEN ──► (moves through stages) ──► WON  ✅  → closedAt set
 *                                 └──► LOST ❌  → closedAt set
 * </pre>
 * <p>When a deal enters a terminal {@link PipelineStep} ({@code isWon} or {@code isLost}),
 * the service layer updates {@code status} and records {@code closedAt}.</p>
 *
 * <h3>Amount</h3>
 * <p>{@code amount} is the estimated or agreed contract value.
 * {@code currency} defaults to "EUR". Both fields are optional at creation
 * and can be refined as the deal progresses.</p>
 *
 * <h3>Database indexes</h3>
 * <ul>
 *   <li>{@code idx_deal_public_id}  — fast lookup by UUID (API)</li>
 *   <li>{@code idx_deal_contact}    — all deals for a contact</li>
 *   <li>{@code idx_deal_stage}      — all deals at a given stage (Kanban)</li>
 *   <li>{@code idx_deal_assigned}   — all deals assigned to a commercial</li>
 *   <li>{@code idx_deal_status}     — filter open / won / lost</li>
 * </ul>
 *
 * @see Contact
 * @see Organisation
 * @see Pipeline
 * @see PipelineStep
 * @see DealStatus
 */
@Entity
@Table(
    name = "crm_deal",
    indexes = {
        @Index(name = "idx_deal_public_id", columnList = "public_id"),
        @Index(name = "idx_deal_contact",   columnList = "contact_id"),
        @Index(name = "idx_deal_stage",     columnList = "stage_id"),
        @Index(name = "idx_deal_assigned",  columnList = "assigned_to_id"),
        @Index(name = "idx_deal_status",    columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class Deal extends BaseEntity<Long> {

    // =========================================================================
    // Identity
    // =========================================================================

    /**
     * Short descriptive title of the deal.
     *
     * <p>Required. Shown on Kanban cards and in deal lists.
     * Typically combines the service and the organisation name.</p>
     *
     * <p>Example: "Nettoyage bureaux — ACME SA"</p>
     */
    @Column(name = "title", nullable = false, length = 200)
    @ToString.Include
    private String title;

    // =========================================================================
    // Financial
    // =========================================================================

    /**
     * Estimated or agreed contract value.
     *
     * <p>Optional at creation — can be filled in once a quote is sent.
     * Uses {@link BigDecimal} for precision (never {@code double} for money).</p>
     *
     * <p>Example: 1100.00 (monthly cleaning contract)</p>
     */
    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    /**
     * ISO 4217 currency code.
     *
     * <p>Defaults to "EUR". Stored as a plain string to avoid a
     * dependency on a currency library for now.</p>
     *
     * <p>Example: "EUR", "USD", "GBP"</p>
     */
    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "EUR";

    // =========================================================================
    // Status & dates
    // =========================================================================

    /**
     * Current lifecycle status of the deal.
     *
     * <p>Required. Defaults to {@link DealStatus#OPEN} on creation.
     * Transitions to {@code WON} or {@code LOST} are triggered by the service layer
     * when the deal enters a terminal {@link PipelineStep}.</p>
     *
     * @see DealStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private DealStatus status = DealStatus.OPEN;

    /**
     * Expected closing date of the deal.
     *
     * <p>Optional. Used in revenue forecasting and overdue deal reports.
     * Stored as {@link LocalDate} — time of day is irrelevant for a closing date.</p>
     */
    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    /**
     * Actual date and time when the deal was closed (won or lost).
     *
     * <p>Set by the service layer when the deal enters a terminal stage.
     * Null if the deal is still open.</p>
     */
    @Column(name = "closed_at")
    private Instant closedAt;

    // =========================================================================
    // Relationships
    // =========================================================================

    /**
     * Pipeline this deal belongs to.
     *
     * <p>Required. Defines the set of stages available for this deal.
     * Loaded lazily — the pipeline structure is only needed in the detail view.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id", nullable = false)
    private Pipeline pipeline;

    /**
     * Current stage of the deal within its pipeline.
     *
     * <p>Required. Updated by the service layer on each stage transition.
     * Must always belong to {@code pipeline.stages}.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private PipelineStep pipelineStep;

    /**
     * Primary contact for this deal.
     *
     * <p>Required. The person the commercial team communicates with
     * to move the deal forward.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    /**
     * Organisation associated with this deal.
     *
     * <p>Optional — a deal can involve an independent contact without a company.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    /**
     * Commercial (User) responsible for this deal.
     *
     * <p>Required. The person in charge of moving the deal forward.
     * Used for task assignment, notifications, and performance reporting.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id", nullable = false)
    private User assignedTo;

    // =========================================================================
    // Internal notes
    // =========================================================================

    /**
     * Free-text internal notes about this deal.
     *
     * <p>Not visible to the client. Complements the structured {@link Interaction}
     * log for quick context that does not warrant a full activity entry.</p>
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // =========================================================================
    // Utility methods
    // =========================================================================

    /**
     * Returns {@code true} if this deal has been closed (won or lost).
     *
     * @return true if {@code status} is {@code WON} or {@code LOST}
     */
    public boolean isClosed() {
        return this.status == DealStatus.WON || this.status == DealStatus.LOST;
    }

    /**
     * Returns {@code true} if the deal has passed its expected closing date
     * without being closed.
     *
     * @return true if open and {@code expectedCloseDate} is in the past
     */
    public boolean isOverdue() {
        return !isClosed()
            && this.expectedCloseDate != null
            && this.expectedCloseDate.isBefore(LocalDate.now());
    }
}