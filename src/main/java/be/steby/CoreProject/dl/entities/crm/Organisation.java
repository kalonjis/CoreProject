package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.BaseEntity;
import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.enums.crm.OrganisationSize;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a company or organisation linked to one or more CRM contacts.
 *
 * <p>An {@code Organisation} is the business entity behind a {@link Contact}.
 * It groups contacts from the same company and provides shared context
 * (industry, size, website) used during deal qualification.</p>
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
 *   <li>One {@code Organisation} → many {@link Contact}</li>
 *   <li>One {@code Organisation} → many {@link Deal}</li>
 *   <li>One {@code Organisation} → one {@link Address} (optional, physical site address)</li>
 * </ul>
 *
 * <h3>Design decisions</h3>
 * <ul>
 *   <li>Address is optional — not all organisations have a known address at creation time.</li>
 *   <li>{@code size} uses an enum to allow filtering/reporting by company size bucket.</li>
 *   <li>{@code notes} is a free-text field for internal comments by the commercial team.</li>
 * </ul>
 *
 * <h3>Database indexes</h3>
 * <ul>
 *   <li>{@code idx_org_public_id} — fast lookup by public UUID (API)</li>
 *   <li>{@code idx_org_name} — search/autocomplete by name</li>
 * </ul>
 *
 * @see Contact
 * @see Deal
 * @see Address
 * @see OrganisationSize
 */
@Entity
@Table(
    name = "crm_organisation",
    indexes = {
        @Index(name = "idx_org_public_id", columnList = "public_id"),
        @Index(name = "idx_org_name",      columnList = "name")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class Organisation extends BaseEntity<Long> {

    // =========================================================================
    // Identity
    // =========================================================================

    /**
     * Legal or commercial name of the organisation.
     *
     * <p>Required. Used in all UI lists, deal titles, and email templates.</p>
     *
     * <p>Example: "ACME SA", "Groupe Dupont SPRL"</p>
     */
    @Column(name = "name", nullable = false, length = 150)
    @ToString.Include
    private String name;

    /**
     * Public website URL of the organisation.
     *
     * <p>Optional. Stored as plain string; validation is handled at the service layer.</p>
     *
     * <p>Example: "https://www.acme.com"</p>
     */
    @Column(name = "website", length = 255)
    private String website;

    // =========================================================================
    // Classification
    // =========================================================================

    /**
     * Industry or sector of activity.
     *
     * <p>Optional free-text. Used for lead qualification and reporting.
     * Can be normalised to an enum later if needed.</p>
     *
     * <p>Example: "Cleaning services", "Real estate", "Healthcare"</p>
     */
    @Column(name = "industry", length = 100)
    private String industry;

    /**
     * Approximate headcount bucket of the organisation.
     *
     * <p>Optional. Helps qualify the deal size and expected revenue.</p>
     *
     * @see OrganisationSize
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "size", length = 20)
    private OrganisationSize size;

    // =========================================================================
    // Contact details
    // =========================================================================

    /**
     * Main switchboard or reception phone number.
     *
     * <p>Optional. Individual contacts have their own phone numbers.
     * This field holds the company-level number.</p>
     *
     * <p>Example: "+32 2 123 45 67"</p>
     */
    @Column(name = "phone", length = 30)
    private String phone;

    // =========================================================================
    // Location
    // =========================================================================

    /**
     * Physical address of the organisation's main site.
     *
     * <p>Optional. Uses the shared {@link Address} entity to avoid duplication.
     * Loaded lazily to avoid unnecessary joins in list queries.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;

    // =========================================================================
    // Internal notes
    // =========================================================================

    /**
     * Free-text internal notes written by the commercial team.
     *
     * <p>Not visible to the client. Useful for context that does not fit
     * in any structured field (e.g., "CEO is a former partner of Pierre").</p>
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}