package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.BaseEntity;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.dl.enums.crm.LeadStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Optional;

/**
 * Entity representing a public inquiry submitted through the contact form
 * by an anonymous visitor.
 *
 * <p>A {@code Lead} is the entry point of the CRM pipeline. Once qualified
 * by the commercial team, it is converted into a
 * {@link be.steby.CoreProject.dl.entities.crm.Contact}, optionally linked
 * to an {@link be.steby.CoreProject.dl.entities.crm.Organisation}, and
 * associated with a {@link be.steby.CoreProject.dl.entities.crm.Deal}.</p>
 *
 * <h4>Lifecycle</h4>
 * <pre>
 * NEW ──► IN_REVIEW ──► CONVERTED  (→ Contact created)
 *                   └──► REJECTED  (spam, out of scope, etc.)
 * </pre>
 *
 * <h4>Privacy</h4>
 * <p>The actual message content is sent via email and intentionally not
 * persisted in the database for privacy reasons.</p>
 *
 * <h4>Anti-spam</h4>
 * <p>{@code ipAddress} and {@code submittedAt} are used for rate limiting
 * and abuse detection.</p>
 *
 * <h4>Name handling</h4>
 * <p>The visitor's name is stored as a single field to avoid unreliable
 * firstname/lastname splitting (e.g. "Jean-Pierre De La Tour").</p>
 *
 * @see LeadType
 * @see LeadStatus
 */
@Entity
@Table(indexes = {
        @Index(name = "idx_lead_email",        columnList = "email"),
        @Index(name = "idx_lead_submitted_at", columnList = "submitted_at"),
        @Index(name = "idx_lead_type",         columnList = "lead_type"),
        @Index(name = "idx_lead_ip",           columnList = "ip_address"),
        @Index(name = "idx_lead_status",       columnList = "status"),
        @Index(name = "idx_lead_assigned",     columnList = "assigned_to_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Lead extends BaseEntity<Long> {

    // ========================================
    // Contact Information
    // ========================================

    /**
     * Email address of the person submitting the inquiry.
     *
     * <p>Required. Used as the reply-to address for responses.</p>
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * Phone number of the person submitting the inquiry.
     *
     * <p>Optional. Helps to pre-fill the Contact.</p>
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * Full name of the person submitting the inquiry, as a single field.
     *
     * <p>Optional. Stored as provided by the visitor — no firstname/lastname
     * split is attempted to avoid unreliable parsing of culturally diverse names.</p>
     */
    @Column(name = "name", length = 200)
    private String name;

    /**
     * Subject or title of the inquiry.
     *
     * <p>Required. Briefly describes the purpose of the inquiry.</p>
     */
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    // ========================================
    // Classification
    // ========================================

    /**
     * Type of inquiry used to route it to the appropriate department.
     *
     * @see LeadType
     */
    @Column(name = "lead_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LeadType leadType;

    // ========================================
    // CRM Status
    // ========================================

    /**
     * Current processing status of the lead in the CRM queue.
     *
     * <p>Required. Defaults to {@link LeadStatus#NEW} on creation.
     * Updated by the commercial team as they review and qualify the lead.</p>
     *
     * @see LeadStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private LeadStatus status = LeadStatus.NEW;

    /**
     * The commercial (User) assigned to process this lead.
     *
     * <p>Optional at creation — assigned by an admin or automatically
     * by the routing rules based on {@code leadType}.
     * Loaded lazily to avoid joins in list queries.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    /**
     * Timestamp when this lead was converted into a Contact.
     *
     * <p>Set by the service layer when {@code status} transitions to
     * {@link LeadStatus#CONVERTED}. Used to measure team response time.</p>
     */
    @Column(name = "converted_at")
    private Instant convertedAt;

    /**
     * Reason why the lead was rejected, if applicable.
     *
     * <p>Optional. Set by the commercial when marking the lead as
     * {@link LeadStatus#REJECTED}. Useful for reporting and quality
     * improvement (e.g., identifying recurring spam patterns).</p>
     *
     * <p>Example: "Spam", "Out of service area", "Duplicate inquiry"</p>
     */
    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    // ========================================
    // Anti-Spam Tracking
    // ========================================

    /**
     * IP address of the client who submitted the form.
     *
     * <p>Used for rate limiting and abuse detection.
     * Supports both IPv4 and IPv6 addresses.</p>
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Timestamp when the form was submitted.
     *
     * <p>Distinct from {@code createdAt} (entity persistence time).
     * Used for rate limiting and audit purposes.</p>
     */
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    // ========================================
    // Utility methods
    // ========================================

    /**
     * Returns {@code true} if a name was provided with the inquiry.
     *
     * @return true if {@code name} is not null and not blank
     */
    public boolean hasName() {
        return this.name != null && !this.name.isBlank();
    }

    /**
     * Returns the visitor's name if provided.
     *
     * @return an {@link Optional} containing the name,
     *         or {@link Optional#empty()} if not provided
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name).filter(n -> !n.isBlank());
    }

    /**
     * Returns {@code true} if this lead has already been converted to a Contact.
     *
     * @return true if {@code status} is {@link LeadStatus#CONVERTED}
     */
    public boolean isConverted() {
        return this.status == LeadStatus.CONVERTED;
    }
}