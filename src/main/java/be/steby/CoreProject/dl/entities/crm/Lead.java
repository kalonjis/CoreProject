package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.BaseEntity;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.dl.enums.crm.Civility;
import be.steby.CoreProject.dl.enums.crm.LeadSource;
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
 * <h4>Anti-spam</h4>
 * <p>{@code ipAddress} and {@code submittedAt} are used for rate limiting
 * and abuse detection.</p>
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
     * Civility (salutation) of the person submitting the inquiry.
     *
     * <p>Optional. Provided by the visitor on the contact form or
     * enriched by the commercial team.</p>
     */
    @Column(name = "civility", length = 10)
    @Enumerated(EnumType.STRING)
    private Civility civility;

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
     * First name of the person, set during commercial enrichment.
     *
     * <p>Optional. Enriched by the commercial team after lead submission,
     * or pre-filled if the visitor provided it on the contact form.</p>
     */
    @Column(name = "first_name", length = 100)
    private String firstName;

    /**
     * Last name of the person, set during commercial enrichment.
     *
     * <p>Optional. Enriched by the commercial team after lead submission,
     * or pre-filled if the visitor provided it on the contact form.</p>
     */
    @Column(name = "last_name", length = 100)
    private String lastName;

    /**
     * Name of the organisation the visitor represents.
     *
     * <p>Optional. Enriched by the commercial team or provided by the visitor.
     * Used at conversion time to create or link an {@link be.steby.CoreProject.dl.entities.crm.Organisation}.</p>
     */
    @Column(name = "organisation_name", length = 200)
    private String organisationName;

    /**
     * Subject or title of the inquiry.
     *
     * <p>Required. Briefly describes the purpose of the inquiry.</p>
     */
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    /**
     * Message body of the inquiry, as submitted by the visitor.
     *
     * <p>Optional on enrichment. Stored to allow the commercial team
     * to read the original message directly in the CRM.</p>
     */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

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

    /**
     * Acquisition source — how the prospect reached us.
     *
     * <p>Automatically detected from UTM parameters on the public contact form.
     * Can be corrected by the commercial during enrichment.</p>
     *
     * @see LeadSource
     */
    @Column(name = "lead_source", length = 30)
    @Enumerated(EnumType.STRING)
    private LeadSource leadSource;

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
     * Returns {@code true} if any name information is available for this lead.
     *
     * @return true if at least one of {@code firstName} or {@code lastName} is not blank
     */
    public boolean hasName() {
        return isPresent(firstName) || isPresent(lastName);
    }

    /**
     * Returns the best available display name for this lead.
     *
     * @return an {@link Optional} containing "firstName lastName" (trimmed), or empty if neither is set
     */
    public Optional<String> getDisplayName() {
        if (isPresent(firstName) || isPresent(lastName)) {
            String full = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
            return Optional.of(full);
        }
        return Optional.empty();
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
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