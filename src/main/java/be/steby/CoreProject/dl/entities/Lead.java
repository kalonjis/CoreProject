package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.LeadType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity representing a public inquiry from anonymous visitors.
 *
 * <p>Stores inquiries submitted through the public contact form by
 * visitors who are not logged in. The actual message content is sent
 * via email and not persisted in the database for privacy reasons.</p>
 *
 * <h4>Use cases:</h4>
 * <ul>
 *   <li>General information requests</li>
 *   <li>Pre-sale / commercial inquiries</li>
 *   <li>Partnership proposals</li>
 *   <li>Press / media inquiries</li>
 * </ul>
 *
 * <h4>Anti-spam:</h4>
 * <p>The {@code ipAddress} and {@code submittedAt} fields are used for
 * rate limiting and abuse detection.</p>
 *
 * <p>For authenticated users, see {@link SupportTicket} instead.</p>
 *
 * @see LeadType
 * @see SupportTicket
 */
@Entity
@Table(name = "public_inquiry",
        indexes = {
                @Index(name = "idx_lead_email", columnList = "email"),
                @Index(name = "idx_lead_submitted_at", columnList = "submitted_at"),
                @Index(name = "idx_lead_type", columnList = "lead_type"),
                @Index(name = "idx_lead_ip", columnList = "ip_address")
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
    // region Contact Information
    // ========================================

    /**
     * Email address of the person submitting the inquiry.
     *
     * <p>Required field. Used as the reply-to address for responses.</p>
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * Name of the person submitting the inquiry.
     *
     * <p>Optional field. Helps personalize responses.</p>
     */
    @Column(name = "name", length = 100)
    private String name;

    /**
     * Subject or title of the inquiry.
     *
     * <p>Required field. Briefly describes the purpose of the inquiry.</p>
     */
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    // ========================================
    // region Classification
    // ========================================

    /**
     * Type of inquiry.
     *
     * <p>Used to route the inquiry to the appropriate department.</p>
     */
    @Column(name = "lead_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LeadType leadType;

    // ========================================
    // region Anti-Spam Tracking
    // ========================================

    /**
     * IP address of the client submitting the form.
     *
     * <p>Used for rate limiting and abuse detection.
     * Supports both IPv4 and IPv6 addresses.</p>
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Timestamp when the form was submitted.
     *
     * <p>Used for rate limiting and audit purposes.
     * Distinct from {@code createdAt} which tracks entity persistence.</p>
     */
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    // ========================================
    // region Utility Methods
    // ========================================

    /**
     * Checks if this inquiry has a name provided.
     *
     * @return true if name is not null and not blank
     */
    public boolean hasName() {
        return this.name != null && !this.name.isBlank();
    }
}