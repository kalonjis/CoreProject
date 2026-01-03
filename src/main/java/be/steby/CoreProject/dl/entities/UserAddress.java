package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.AddressType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entity linking addresses to users.
 *
 * <p>This entity represents the many-to-many relationship between {@link User}
 * and {@link Address}, with additional metadata specific to user addresses.</p>
 *
 * <h4>Relationship structure:</h4>
 * <pre>
 * User (1) ←──→ (*) UserAddress (*) ←──→ (1) Address
 * </pre>
 *
 * <h4>Key features:</h4>
 * <ul>
 *   <li>A user can have multiple addresses (home, work, etc.)</li>
 *   <li>Multiple users can share the same physical address</li>
 *   <li>Each link can have different metadata (type, label, default status)</li>
 *   <li>Supports address history through validity periods</li>
 * </ul>
 *
 * <h4>Usage examples:</h4>
 * <pre>
 * // User A and User B (roommates) share the same address
 * Address sharedAddress = new Address(...);
 * 
 * UserAddress userALink = new UserAddress(userA, sharedAddress, AddressType.RESIDENTIAL);
 * userALink.setLabel("Home");
 * userALink.setDefault(true);
 * 
 * UserAddress userBLink = new UserAddress(userB, sharedAddress, AddressType.RESIDENTIAL);
 * userBLink.setLabel("Shared Apartment");
 * userBLink.setDefault(true);
 * </pre>
 *
 * <h4>Default address constraints:</h4>
 * <p>Business rule: A user should have at most one default address per {@link AddressType}.
 * This constraint should be enforced at the service layer when setting an address as default.</p>
 *
 * @see AddressLink
 * @see Address
 * @see User
 */
@Entity
@Table(name = "user_address",
        indexes = {
                @Index(name = "idx_user_address_user", columnList = "user_id"),
                @Index(name = "idx_user_address_user_type", columnList = "user_id, address_type"),
                @Index(name = "idx_user_address_user_default", columnList = "user_id, is_default")
        },
        uniqueConstraints = {
                // Optional: Prevent duplicate links between same user and address with same type
                // Uncomment if this business rule is desired
                // @UniqueConstraint(
                //         name = "uk_user_address_user_address_type",
                //         columnNames = {"user_id", "address_id", "address_type"}
                // )
        })
@DiscriminatorValue("USER")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"user"})
@ToString(callSuper = true, exclude = {"user"})
public class UserAddress extends AddressLink {

    // ========================================
    // region User Reference
    // ========================================

    /**
     * The user who owns this address link.
     *
     * <p>This establishes the ownership of the address relationship.
     * The user can have multiple UserAddress entries pointing to
     * different (or even the same) addresses.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // endregion

    // ========================================
    // region User-Specific Fields
    // ========================================

    /**
     * Indicates if this is the user's primary contact address.
     *
     * <p>Distinct from {@link AddressLink#isDefault()} which is per-type.
     * This flag indicates the overall primary address for the user,
     * regardless of address type.</p>
     *
     * <p>A user should have at most one primary address.
     * This constraint should be enforced at the service layer.</p>
     */
    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    /**
     * Whether this address can be used for billing purposes.
     *
     * <p>An address might be valid for shipping but not for billing
     * (e.g., a PO Box or a workplace address).</p>
     */
    @Column(name = "billing_eligible", nullable = false)
    private boolean billingEligible = true;

    /**
     * Whether this address can be used for shipping purposes.
     *
     * <p>Some addresses might not be suitable for shipping
     * (e.g., a billing-only address).</p>
     */
    @Column(name = "shipping_eligible", nullable = false)
    private boolean shippingEligible = true;

    // endregion

    // ========================================
    // region Constructors
    // ========================================

    /**
     * Creates a new UserAddress link with minimum required fields.
     *
     * @param user        the user who owns this address link
     * @param address     the address being linked
     * @param addressType the type/purpose of this address
     */
    public UserAddress(User user, Address address, AddressType addressType) {
        this.user = user;
        this.setAddress(address);
        this.setAddressType(addressType);
        this.setActive(true);
    }

    /**
     * Creates a new UserAddress link with label.
     *
     * @param user        the user who owns this address link
     * @param address     the address being linked
     * @param addressType the type/purpose of this address
     * @param label       user-friendly label for the address
     */
    public UserAddress(User user, Address address, AddressType addressType, String label) {
        this(user, address, addressType);
        this.setLabel(label);
    }

    /**
     * Creates a new UserAddress link with full configuration.
     *
     * @param user        the user who owns this address link
     * @param address     the address being linked
     * @param addressType the type/purpose of this address
     * @param label       user-friendly label for the address
     * @param isDefault   whether this is the default address for its type
     * @param isPrimary   whether this is the user's primary address
     */
    public UserAddress(User user, Address address, AddressType addressType,
                       String label, boolean isDefault, boolean isPrimary) {
        this(user, address, addressType, label);
        this.setDefault(isDefault);
        this.isPrimary = isPrimary;
    }

    // endregion

    // ========================================
    // region Convenience Methods
    // ========================================

    /**
     * Checks if this address can be used as a billing address.
     *
     * <p>An address is usable for billing if it is:</p>
     * <ul>
     *   <li>Currently valid (active and within validity period)</li>
     *   <li>Marked as billing eligible</li>
     * </ul>
     *
     * @return true if this address can be used for billing
     */
    public boolean canUseForBilling() {
        return isCurrentlyValid() && billingEligible;
    }

    /**
     * Checks if this address can be used as a shipping address.
     *
     * <p>An address is usable for shipping if it is:</p>
     * <ul>
     *   <li>Currently valid (active and within validity period)</li>
     *   <li>Marked as shipping eligible</li>
     *   <li>Has a complete underlying address</li>
     * </ul>
     *
     * @return true if this address can be used for shipping
     */
    public boolean canUseForShipping() {
        return isCurrentlyValid()
                && shippingEligible
                && getAddress() != null
                && getAddress().isComplete();
    }

    /**
     * Sets this address as the user's primary address.
     *
     * <p><b>Note:</b> This only sets the flag on this entity.
     * The service layer is responsible for unsetting the primary flag
     * on any other addresses for the same user.</p>
     */
    public void markAsPrimary() {
        this.isPrimary = true;
    }

    /**
     * Removes the primary status from this address.
     */
    public void unmarkAsPrimary() {
        this.isPrimary = false;
    }

    /**
     * Gets a display name for this address.
     *
     * <p>Returns the label if set, otherwise generates a description
     * based on the address type.</p>
     *
     * @return a human-readable name for this address
     */
    public String getDisplayName() {
        if (getLabel() != null && !getLabel().isBlank()) {
            return getLabel();
        }

        String typeName = getAddressType() != null
                ? getAddressType().name().toLowerCase().replace("_", " ")
                : "address";

        if (isPrimary) {
            return "Primary " + typeName;
        }

        if (isDefault()) {
            return "Default " + typeName;
        }

        return typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
    }

    // endregion
}