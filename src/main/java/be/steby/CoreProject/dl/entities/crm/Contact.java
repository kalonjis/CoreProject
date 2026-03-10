package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.BaseEntity;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.crm.ContactStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a physical person tracked in the CRM.
 *
 * <p>A {@code Contact} is typically created by converting a {@link Lead} once
 * the commercial team decides to engage. It holds all identifying and
 * relationship information for a prospect or client.</p>
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
 *   <li>Many {@code Contact} → one {@link Organisation} (optional — a contact may be independent)</li>
 *   <li>One {@code Contact} → one {@link User} (optional — linked once the contact creates an account)</li>
 *   <li>One {@code Contact} → one {@link Lead} (optional — traceability of the conversion origin)</li>
 *   <li>One {@code Contact} → many {@link Deal}</li>
 *   <li>One {@code Contact} → many {@link Interaction}</li>
 * </ul>
 *
 * <h3>Lead conversion flow</h3>
 * <pre>
 * Lead (anonymous visitor)
 *   └──► Contact (qualified person)
 *           └──► Deal (commercial opportunity)
 * </pre>
 *
 * <h3>User link</h3>
 * <p>When a contact signs a contract and creates an account on the platform,
 * the {@code linkedUser} field is populated. This bridges the CRM domain
 * with the authentication domain without merging them.</p>
 *
 * <h3>Database indexes</h3>
 * <ul>
 *   <li>{@code idx_contact_public_id} — fast lookup by UUID (API)</li>
 *   <li>{@code idx_contact_email} — search by email, uniqueness check</li>
 *   <li>{@code idx_contact_org} — fetch all contacts of an organisation</li>
 *   <li>{@code idx_contact_status} — filter by pipeline status</li>
 * </ul>
 *
 * @see Lead
 * @see Organisation
 * @see Deal
 * @see ContactStatus
 */
@Entity
@Table(
    name = "crm_contact",
    indexes = {
        @Index(name = "idx_contact_public_id", columnList = "public_id"),
        @Index(name = "idx_contact_email",     columnList = "email"),
        @Index(name = "idx_contact_org",       columnList = "organisation_id"),
        @Index(name = "idx_contact_assigned", columnList = "assigned_to_id"),
        @Index(name = "idx_contact_status",    columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class Contact extends BaseEntity<Long> {

    // =========================================================================
    // Identity
    // =========================================================================

    /**
     * First name of the contact.
     *
     * <p>Required. Used in email templates, notifications, and UI display.</p>
     *
     * <p>Example: "Thomas"</p>
     */
    @Column(name = "first_name", nullable = false, length = 50)
    @ToString.Include
    private String firstName;

    /**
     * Last name of the contact.
     *
     * <p>Required. Combined with {@code firstName} for full name display.</p>
     *
     * <p>Example: "Dupont"</p>
     */
    @Column(name = "last_name", nullable = false, length = 50)
    @ToString.Include
    private String lastName;

    /**
     * Professional email address of the contact.
     *
     * <p>Required. Used as the primary communication channel and deduplication key.
     * Should be unique across all contacts.</p>
     *
     * <p>Example: "thomas.dupont@acme.com"</p>
     */
    @Column(name = "email", nullable = false, length = 254)
    @ToString.Include
    private String email;

    /**
     * Direct phone number of the contact.
     *
     * <p>Optional. Distinct from the organisation's switchboard number.
     * Stored as plain string to support international formats.</p>
     *
     * <p>Example: "+32 475 12 34 56"</p>
     */
    @Column(name = "phone", length = 30)
    private String phone;

    /**
     * Job title or position within their organisation.
     *
     * <p>Optional. Useful for qualifying the contact's decision-making authority
     * (e.g., "Facility Manager" vs "CEO").</p>
     *
     * <p>Example: "Facility Manager", "CEO", "Purchasing Director"</p>
     */
    @Column(name = "job_title", length = 100)
    private String jobTitle;

    // =========================================================================
    // CRM Status
    // =========================================================================

    /**
     * Current lifecycle status of the contact in the CRM pipeline.
     *
     * <p>Required. Defaults to {@link ContactStatus#NEW} on creation.
     * Transitions are managed by the service layer based on deal outcomes.</p>
     *
     * @see ContactStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ContactStatus status = ContactStatus.NEW;


    // =========================================================================
    // Assignment
    // =========================================================================

    /**
     * The commercial (User) responsible for this contact.
     *
     * <p>Optional at creation — inherited from {@link Lead#getAssignedTo()}
     * when converted, or set manually afterward.
     * Loaded lazily to avoid unnecessary joins in list queries.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    // =========================================================================
    // Relationships
    // =========================================================================

    /**
     * Organisation this contact belongs to.
     *
     * <p>Optional — a contact can be independent (e.g., sole trader).
     * Loaded lazily to avoid unnecessary joins in list queries.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    /**
     * The originating lead that was converted into this contact.
     *
     * <p>Optional. Provides full traceability from the first anonymous
     * form submission to the qualified contact. Set once at conversion
     * and never updated.</p>
     *
     * <p>Loaded lazily — only needed in the contact detail view.</p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", unique = true)
    private Lead originLead;

    /**
     * Platform account linked to this contact.
     *
     * <p>Optional. Populated when the contact creates an account on the platform
     * after signing a contract. Bridges the CRM domain and the auth domain.</p>
     *
     * <p>Loaded lazily — only needed in the contact detail view.</p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User linkedUser;

    // =========================================================================
    // Internal notes
    // =========================================================================

    /**
     * Free-text internal notes written by the commercial team.
     *
     * <p>Not visible to the contact. Used for context, preferences, and
     * any information that does not fit in a structured field.</p>
     *
     * <p>Example: "Prefers contact by email, available mornings only."</p>
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // =========================================================================
    // Utility methods
    // =========================================================================

    /**
     * Returns the full display name of the contact.
     *
     * @return "{firstName} {lastName}"
     */
    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }

    /**
     * Returns {@code true} if this contact has been linked to a platform account.
     *
     * @return true if {@code linkedUser} is not null
     */
    public boolean hasLinkedUser() {
        return this.linkedUser != null;
    }

    /**
     * Returns {@code true} if this contact was converted from a lead.
     *
     * @return true if {@code originLead} is not null
     */
    public boolean isConvertedFromLead() {
        return this.originLead != null;
    }
}