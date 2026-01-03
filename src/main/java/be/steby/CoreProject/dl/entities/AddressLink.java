package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.entities.BaseEntity;
import be.steby.CoreProject.dl.enums.AddressModificationStrategy;
import be.steby.CoreProject.dl.enums.AddressType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Abstract base entity for linking addresses to other entities.
 *
 * <p>This class serves as the foundation for all address relationships in the system.
 * It contains common metadata about the relationship between an {@link Address}
 * and its owning entity, while allowing specialized subclasses to define the
 * specific owner type.</p>
 *
 * <h4>Design pattern:</h4>
 * <p>This implements a flexible many-to-many relationship pattern where:</p>
 * <ul>
 *   <li>An {@link Address} can be linked to multiple entities (users, companies, etc.)</li>
 *   <li>An entity can have multiple addresses</li>
 *   <li>Each link carries its own metadata (type, label, validity period, etc.)</li>
 * </ul>
 *
 * <h4>Inheritance strategy:</h4>
 * <p>Uses {@code JOINED} inheritance to allow each subclass to have its own table
 * while sharing common fields. This provides:</p>
 * <ul>
 *   <li>Clean separation of concerns</li>
 *   <li>Efficient queries when filtering by owner type</li>
 *   <li>Flexibility to add subclass-specific fields</li>
 * </ul>
 *
 * <h4>Subclasses:</h4>
 * <ul>
 *   <li>{@link UserAddress} - Links addresses to users</li>
 *   <li>CompanyAddress - Links addresses to companies (future)</li>
 *   <li>OrderAddress - Links/snapshots addresses for orders (future)</li>
 * </ul>
 *
 * @see Address
 * @see UserAddress
 * @see AddressType
 * @see AddressModificationStrategy
 */
@Entity
@Table(name = "address_link",
        indexes = {
                @Index(name = "idx_address_link_address", columnList = "address_id"),
                @Index(name = "idx_address_link_type", columnList = "address_type"),
                @Index(name = "idx_address_link_default", columnList = "is_default")
        })
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "link_type", discriminatorType = DiscriminatorType.STRING, length = 30)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"address"})
@ToString(callSuper = true, exclude = {"address"})
public abstract class AddressLink extends BaseEntity<Long> {

    // ========================================
    // region Address Reference
    // ========================================

    /**
     * The address being linked.
     *
     * <p>This is the actual {@link Address} entity containing the geographical data.
     * Multiple AddressLink instances can reference the same Address.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    // endregion

    // ========================================
    // region Link Metadata
    // ========================================

    /**
     * The type/purpose of this address for the owning entity.
     *
     * <p>Defines how the address is used (billing, shipping, etc.).
     * The same physical address can have different types for different owners.</p>
     *
     * @see AddressType
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    private AddressType addressType = AddressType.PRIMARY;

    /**
     * Whether this is the default address for its type.
     *
     * <p>Each owner should have at most one default address per type.
     * This constraint should be enforced at the service layer.</p>
     *
     * <p>Example: A user might have multiple shipping addresses, but only
     * one is marked as the default shipping address.</p>
     */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    /**
     * User-defined label for this address.
     *
     * <p>Allows users to give meaningful names to their addresses
     * for easy identification.</p>
     *
     * <p>Examples: "Home", "Office", "Parents' house", "Vacation home"</p>
     */
    @Column(name = "label", length = 50)
    private String label;

    /**
     * Optional notes or instructions related to this address.
     *
     * <p>Can contain delivery instructions, access codes, or any
     * other relevant information.</p>
     *
     * <p>Examples: "Ring doorbell twice", "Leave at back door", "Call before delivery"</p>
     */
    @Column(name = "notes", length = 500)
    private String notes;

    // endregion

    // ========================================
    // region Modification Strategy
    // ========================================

    /**
     * The modification strategy for this address link.
     *
     * <p>Determines how changes to the underlying address are handled.
     * Can override the system default on a per-link basis.</p>
     *
     * <p>If null, the system default strategy should be applied.</p>
     *
     * @see AddressModificationStrategy
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "modification_strategy", length = 20)
    private AddressModificationStrategy modificationStrategy;

    // endregion

    // ========================================
    // region Validity Period
    // ========================================

    /**
     * Start date of this address link's validity.
     *
     * <p>If set, the address is only considered valid/active from this date.
     * Useful for:</p>
     * <ul>
     *   <li>Scheduled address changes (moving to a new home)</li>
     *   <li>Temporary addresses with known start dates</li>
     *   <li>Historical tracking</li>
     * </ul>
     *
     * <p>If null, the address is valid from creation.</p>
     */
    @Column(name = "valid_from")
    private Instant validFrom;

    /**
     * End date of this address link's validity.
     *
     * <p>If set, the address is only considered valid/active until this date.
     * Useful for:</p>
     * <ul>
     *   <li>Temporary addresses (e.g., vacation homes, temporary relocations)</li>
     *   <li>Soft-deleting addresses while preserving history</li>
     *   <li>Scheduling address expiration</li>
     * </ul>
     *
     * <p>If null, the address has no expiration.</p>
     */
    @Column(name = "valid_to")
    private Instant validTo;

    // endregion

    // ========================================
    // region Status
    // ========================================

    /**
     * Whether this address link is currently active.
     *
     * <p>Allows soft-disabling an address without deleting it.
     * Inactive addresses are typically excluded from normal queries
     * but preserved for historical purposes.</p>
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Whether this address has been verified by the owner.
     *
     * <p>Distinct from {@link Address#isValidated()} which indicates
     * technical validation. This field indicates the owner has confirmed
     * the address is correct and belongs to them.</p>
     */
    @Column(name = "verified_by_owner", nullable = false)
    private boolean verifiedByOwner = false;

    /**
     * Timestamp when the owner verified this address.
     */
    @Column(name = "verified_at")
    private Instant verifiedAt;

    // endregion

    // ========================================
    // region Convenience Methods
    // ========================================

    /**
     * Checks if this address link is currently valid based on validity period.
     *
     * <p>An address is valid if:</p>
     * <ul>
     *   <li>It is active</li>
     *   <li>Current time is after validFrom (if set)</li>
     *   <li>Current time is before validTo (if set)</li>
     * </ul>
     *
     * @return true if the address link is currently valid
     */
    public boolean isCurrentlyValid() {
        if (!active) {
            return false;
        }

        Instant now = Instant.now();

        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }

        if (validTo != null && now.isAfter(validTo)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if this address link is temporary (has an end date).
     *
     * @return true if validTo is set
     */
    public boolean isTemporary() {
        return validTo != null;
    }

    /**
     * Checks if this address link has expired.
     *
     * @return true if validTo is set and in the past
     */
    public boolean isExpired() {
        return validTo != null && Instant.now().isAfter(validTo);
    }

    /**
     * Checks if this address link is scheduled for the future.
     *
     * @return true if validFrom is set and in the future
     */
    public boolean isFuture() {
        return validFrom != null && Instant.now().isBefore(validFrom);
    }

    /**
     * Gets the effective modification strategy.
     *
     * <p>Returns the link-specific strategy if set, otherwise returns
     * the provided default strategy.</p>
     *
     * @param defaultStrategy the system default strategy
     * @return the effective strategy to use
     */
    public AddressModificationStrategy getEffectiveStrategy(AddressModificationStrategy defaultStrategy) {
        return modificationStrategy != null ? modificationStrategy : defaultStrategy;
    }

    /**
     * Marks this address as verified by the owner.
     */
    public void markAsVerified() {
        this.verifiedByOwner = true;
        this.verifiedAt = Instant.now();
    }

    /**
     * Deactivates this address link.
     *
     * <p>Sets active to false and validTo to current time if not already set.</p>
     */
    public void deactivate() {
        this.active = false;
        if (this.validTo == null) {
            this.validTo = Instant.now();
        }
    }

    // endregion
}