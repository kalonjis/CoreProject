package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.BaseEntity;
import be.steby.CoreProject.dl.enums.crm.ContactRole;
import jakarta.persistence.*;
import lombok.*;

/**
 * Join entity representing a {@link Contact}'s involvement in a {@link Deal}.
 *
 * <p>A deal may involve several contacts simultaneously (decision-maker,
 * signer, technical evaluator…). This entity captures the relationship and
 * the contact's {@link ContactRole role} for that deal.</p>
 *
 * <h3>Primary contact rule</h3>
 * <p>Exactly one {@code DealContactRole} per deal must have {@code primary = true}.
 * This constraint is enforced at the service layer — not by a database unique index —
 * to allow atomic swaps (unset current primary, set new primary in one transaction).</p>
 *
 * <h3>Relationships</h3>
 * <ul>
 *   <li>Many {@code DealContactRole} → one {@link Deal}</li>
 *   <li>Many {@code DealContactRole} → one {@link Contact}</li>
 * </ul>
 *
 * <h3>Database indexes</h3>
 * <ul>
 *   <li>{@code idx_dcr_deal}     — all roles for a given deal</li>
 *   <li>{@code idx_dcr_contact}  — all deals involving a given contact</li>
 * </ul>
 */
@Entity
@Table(
    name = "deal_contact_role",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_dcr_deal_contact", columnNames = {"deal_id", "contact_id"})
    },
    indexes = {
        @Index(name = "idx_dcr_deal",    columnList = "deal_id"),
        @Index(name = "idx_dcr_contact", columnList = "contact_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class DealContactRole extends BaseEntity<Long> {

    /**
     * The deal this role belongs to.
     *
     * <p>Required. Loaded lazily — the deal context is available
     * from the caller in all use cases.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id", nullable = false)
    private Deal deal;

    /**
     * The contact involved in this deal.
     *
     * <p>Required. Loaded lazily.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    @ToString.Include
    private Contact contact;

    /**
     * The contact's role on this deal.
     *
     * <p>Defaults to {@link ContactRole#OTHER} if not specified at creation.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    @ToString.Include
    private ContactRole role = ContactRole.OTHER;

    /**
     * Whether this contact is the primary point of contact for the deal.
     *
     * <p>Exactly one {@code DealContactRole} per deal should have this flag set.
     * Enforced by the service layer.</p>
     */
    @Column(name = "is_primary", nullable = false)
    @ToString.Include
    private boolean primary;
}
