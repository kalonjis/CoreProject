package be.steby.CoreProject.dal.specifications;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.UserAddress;
import be.steby.CoreProject.dl.enums.AddressType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/**
 * JPA Specifications for filtering {@link UserAddress} entities.
 *
 * <p>Provides composable filter methods that can be combined using
 * {@code and()}, {@code or()}, and {@code not()} operations.</p>
 *
 * <h4>Usage example:</h4>
 * <pre>
 * Specification<UserAddress> spec = UserAddressSpecification.belongsToUser(user)
 *     .and(UserAddressSpecification.hasType(AddressType.BILLING))
 *     .and(UserAddressSpecification.isActive())
 *     .and(UserAddressSpecification.isBillingEligible());
 *
 * List<UserAddress> results = repository.findAll(spec);
 * </pre>
 *
 * @see UserAddress
 */
public class UserAddressSpecification {

    private UserAddressSpecification() {
        // Utility class
    }

    // ========================================
    // region User Filter
    // ========================================

    /**
     * Filters addresses belonging to a specific user.
     *
     * @param user the user
     * @return specification filtering by user
     */
    public static Specification<UserAddress> belongsToUser(User user) {
        return (root, query, cb) -> cb.equal(root.get("user"), user);
    }

    /**
     * Filters addresses belonging to a user by ID.
     *
     * @param userId the user ID
     * @return specification filtering by user ID
     */
    public static Specification<UserAddress> belongsToUserId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    // endregion

    // ========================================
    // region Type Filters
    // ========================================

    /**
     * Filters addresses by type.
     *
     * @param type the address type
     * @return specification filtering by type
     */
    public static Specification<UserAddress> hasType(AddressType type) {
        return (root, query, cb) -> cb.equal(root.get("addressType"), type);
    }

    // endregion

    // ========================================
    // region Status Filters
    // ========================================

    /**
     * Filters only active addresses.
     *
     * @return specification filtering active addresses
     */
    public static Specification<UserAddress> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    /**
     * Filters only inactive addresses.
     *
     * @return specification filtering inactive addresses
     */
    public static Specification<UserAddress> isInactive() {
        return (root, query, cb) -> cb.isFalse(root.get("active"));
    }

    /**
     * Filters default addresses.
     *
     * @return specification filtering default addresses
     */
    public static Specification<UserAddress> isDefault() {
        return (root, query, cb) -> cb.isTrue(root.get("isDefault"));
    }

    /**
     * Filters non-default addresses.
     *
     * @return specification filtering non-default addresses
     */
    public static Specification<UserAddress> isNotDefault() {
        return (root, query, cb) -> cb.isFalse(root.get("isDefault"));
    }

    /**
     * Filters primary addresses.
     *
     * @return specification filtering primary addresses
     */
    public static Specification<UserAddress> isPrimary() {
        return (root, query, cb) -> cb.isTrue(root.get("isPrimary"));
    }

    /**
     * Filters non-primary addresses.
     *
     * @return specification filtering non-primary addresses
     */
    public static Specification<UserAddress> isNotPrimary() {
        return (root, query, cb) -> cb.isFalse(root.get("isPrimary"));
    }

    /**
     * Filters addresses verified by owner.
     *
     * @return specification filtering verified addresses
     */
    public static Specification<UserAddress> isVerifiedByOwner() {
        return (root, query, cb) -> cb.isTrue(root.get("verifiedByOwner"));
    }

    /**
     * Filters addresses not verified by owner.
     *
     * @return specification filtering non-verified addresses
     */
    public static Specification<UserAddress> isNotVerifiedByOwner() {
        return (root, query, cb) -> cb.isFalse(root.get("verifiedByOwner"));
    }

    // endregion

    // ========================================
    // region Eligibility Filters
    // ========================================

    /**
     * Filters billing-eligible addresses.
     *
     * @return specification filtering billing-eligible addresses
     */
    public static Specification<UserAddress> isBillingEligible() {
        return (root, query, cb) -> cb.isTrue(root.get("billingEligible"));
    }

    /**
     * Filters non-billing-eligible addresses.
     *
     * @return specification filtering non-billing-eligible addresses
     */
    public static Specification<UserAddress> isNotBillingEligible() {
        return (root, query, cb) -> cb.isFalse(root.get("billingEligible"));
    }

    /**
     * Filters shipping-eligible addresses.
     *
     * @return specification filtering shipping-eligible addresses
     */
    public static Specification<UserAddress> isShippingEligible() {
        return (root, query, cb) -> cb.isTrue(root.get("shippingEligible"));
    }

    /**
     * Filters non-shipping-eligible addresses.
     *
     * @return specification filtering non-shipping-eligible addresses
     */
    public static Specification<UserAddress> isNotShippingEligible() {
        return (root, query, cb) -> cb.isFalse(root.get("shippingEligible"));
    }

    // endregion

    // ========================================
    // region Validity Period Filters
    // ========================================

    /**
     * Filters addresses currently valid (within validity period).
     *
     * <p>An address is currently valid if:</p>
     * <ul>
     *   <li>validFrom is null OR validFrom <= now</li>
     *   <li>validTo is null OR validTo > now</li>
     * </ul>
     *
     * @return specification filtering currently valid addresses
     */
    public static Specification<UserAddress> isCurrentlyValid() {
        return (root, query, cb) -> {
            Instant now = Instant.now();
            return cb.and(
                    cb.or(
                            cb.isNull(root.get("validFrom")),
                            cb.lessThanOrEqualTo(root.get("validFrom"), now)
                    ),
                    cb.or(
                            cb.isNull(root.get("validTo")),
                            cb.greaterThan(root.get("validTo"), now)
                    )
            );
        };
    }

    /**
     * Filters expired addresses.
     *
     * @return specification filtering expired addresses
     */
    public static Specification<UserAddress> isExpired() {
        return (root, query, cb) -> {
            Instant now = Instant.now();
            return cb.and(
                    cb.isNotNull(root.get("validTo")),
                    cb.lessThan(root.get("validTo"), now)
            );
        };
    }

    /**
     * Filters future addresses (not yet valid).
     *
     * @return specification filtering future addresses
     */
    public static Specification<UserAddress> isFuture() {
        return (root, query, cb) -> {
            Instant now = Instant.now();
            return cb.and(
                    cb.isNotNull(root.get("validFrom")),
                    cb.greaterThan(root.get("validFrom"), now)
            );
        };
    }

    // endregion

    // ========================================
    // region Address Data Filters
    // ========================================

    /**
     * Filters by country code.
     *
     * @param countryCode the ISO country code
     * @return specification filtering by country
     */
    public static Specification<UserAddress> hasCountry(String countryCode) {
        return (root, query, cb) -> cb.equal(
                cb.upper(root.get("address").get("countryCode")),
                countryCode.toUpperCase()
        );
    }

    /**
     * Filters by city (case-insensitive).
     *
     * @param city the city name
     * @return specification filtering by city
     */
    public static Specification<UserAddress> hasCity(String city) {
        return (root, query, cb) -> cb.equal(
                cb.lower(root.get("address").get("city")),
                city.toLowerCase()
        );
    }

    /**
     * Filters by postal code.
     *
     * @param postalCode the postal code
     * @return specification filtering by postal code
     */
    public static Specification<UserAddress> hasPostalCode(String postalCode) {
        return (root, query, cb) -> cb.equal(
                cb.lower(root.get("address").get("postalCode")),
                postalCode.toLowerCase()
        );
    }

    /**
     * Filters addresses with validated underlying address.
     *
     * @return specification filtering validated addresses
     */
    public static Specification<UserAddress> hasValidatedAddress() {
        return (root, query, cb) -> cb.isTrue(root.get("address").get("validated"));
    }

    /**
     * Filters addresses with geolocation coordinates.
     *
     * @return specification filtering addresses with coordinates
     */
    public static Specification<UserAddress> hasCoordinates() {
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("address").get("latitude")),
                cb.isNotNull(root.get("address").get("longitude"))
        );
    }

    // endregion

    // ========================================
    // region Label Filter
    // ========================================

    /**
     * Filters by label (case-insensitive, contains).
     *
     * @param label the label to search
     * @return specification filtering by label
     */
    public static Specification<UserAddress> labelContains(String label) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("label")),
                "%" + label.toLowerCase() + "%"
        );
    }

    // endregion

    // ========================================
    // region Composite Filters
    // ========================================

    /**
     * Filters active and currently valid addresses for a user.
     *
     * <p>This is the most common filter combination for displaying
     * a user's usable addresses.</p>
     *
     * @param user the user
     * @return combined specification
     */
    public static Specification<UserAddress> activeAndValidForUser(User user) {
        return belongsToUser(user)
                .and(isActive())
                .and(isCurrentlyValid());
    }

    /**
     * Filters billing-ready addresses for a user.
     *
     * @param user the user
     * @return combined specification
     */
    public static Specification<UserAddress> billingReadyForUser(User user) {
        return activeAndValidForUser(user)
                .and(isBillingEligible());
    }

    /**
     * Filters shipping-ready addresses for a user.
     *
     * @param user the user
     * @return combined specification
     */
    public static Specification<UserAddress> shippingReadyForUser(User user) {
        return activeAndValidForUser(user)
                .and(isShippingEligible());
    }

    // endregion
}