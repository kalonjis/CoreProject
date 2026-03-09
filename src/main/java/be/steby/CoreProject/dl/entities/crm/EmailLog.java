package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Stores structured email details for an {@link Interaction} of type {@code EMAIL}.
 *
 * <p>This entity complements the generic {@link Interaction} record with data
 * specific to email interactions: the external message identifier for
 * traceability, a short body excerpt, and optional engagement tracking
 * (open and click events).</p>
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
 * <p>Exactly one {@code EmailLog} → one {@link Interaction} (owning side).
 * The {@code interaction} holds the inverse {@code @OneToOne(mappedBy = "interaction")}.</p>
 *
 * <h3>External message ID</h3>
 * <p>{@code externalMessageId} stores the identifier assigned by the mail
 * provider (SMTP Message-ID header, Gmail message ID, etc.). It allows
 * cross-referencing the CRM record with the actual email in the mailbox
 * or a transactional email provider (e.g. SendGrid, Mailgun).</p>
 *
 * <h3>Engagement tracking</h3>
 * <p>{@code openedAt} and {@code clickedAt} are populated by webhook callbacks
 * from the email provider when tracking pixels or tracked links are used.
 * Both are optional and only relevant for outbound emails.</p>
 *
 * <h3>Database indexes</h3>
 * <ul>
 *   <li>{@code idx_emaillog_activity}    — join from interaction to its EmailLog</li>
 *   <li>{@code idx_emaillog_external_id} — lookup by provider message ID (webhook callbacks)</li>
 * </ul>
 *
 * @see Interaction
 */
@Entity
@Table(
    name = "crm_email_log",
    indexes = {
        @Index(name = "idx_emaillog_interaction",    columnList = "interaction_id"),
        @Index(name = "idx_emaillog_external_id", columnList = "external_message_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class EmailLog extends BaseEntity<Long> {

    // =========================================================================
    // Email identification
    // =========================================================================

    /**
     * Subject line of the email.
     *
     * <p>Required. Stored here for quick display in the timeline without
     * having to fetch the full email from the mail provider.</p>
     *
     * <p>Example: "Quote for cleaning services — ACME SA"</p>
     */
    @Column(name = "subject", nullable = false, length = 255)
    @ToString.Include
    private String subject;

    /**
     * Short excerpt of the email body (first ~300 characters).
     *
     * <p>Optional. Used as a preview in the timeline card.
     * The full email body is NOT stored — it remains in the mail provider.</p>
     */
    @Column(name = "body_snippet", length = 300)
    private String bodySnippet;

    /**
     * Message identifier assigned by the mail provider.
     *
     * <p>Optional. Allows cross-referencing this CRM record with the actual
     * email thread in the mailbox or provider dashboard (SMTP Message-ID
     * header, Gmail message ID, SendGrid message ID, etc.).</p>
     *
     * <p>Also used as the correlation key when processing open/click
     * webhook callbacks from the provider.</p>
     */
    @Column(name = "external_message_id", length = 255)
    private String externalMessageId;

    // =========================================================================
    // Engagement tracking
    // =========================================================================

    /**
     * Timestamp when the recipient first opened the email.
     *
     * <p>Optional. Populated via a webhook callback from the email provider
     * when a tracking pixel is embedded. Null if not opened or not tracked.</p>
     */
    @Column(name = "opened_at")
    private Instant openedAt;

    /**
     * Timestamp when the recipient first clicked a tracked link in the email.
     *
     * <p>Optional. Populated via a webhook callback from the email provider.
     * Null if no link was clicked or tracking is not enabled.</p>
     */
    @Column(name = "clicked_at")
    private Instant clickedAt;

    // =========================================================================
    // Relationship
    // =========================================================================

    /**
     * The parent interaction this email log belongs to.
     *
     * <p>Required. Owning side of the {@code interaction ↔ EmailLog} relation.</p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interaction_id", nullable = false, unique = true)
    private Interaction interaction;

    // =========================================================================
    // Utility methods
    // =========================================================================

    /**
     * Returns {@code true} if the email has been opened by the recipient.
     *
     * @return true if {@code openedAt} is not null
     */
    public boolean wasOpened() {
        return this.openedAt != null;
    }

    /**
     * Returns {@code true} if the recipient clicked a tracked link.
     *
     * @return true if {@code clickedAt} is not null
     */
    public boolean wasClicked() {
        return this.clickedAt != null;
    }
}