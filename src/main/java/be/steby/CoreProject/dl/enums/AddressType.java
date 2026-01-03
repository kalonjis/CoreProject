package be.steby.CoreProject.dl.enums;

/**
 * Enumeration representing the different types of addresses in the system.
 *
 * <p>This enum is used to categorize addresses based on their purpose or function.
 * It is designed to be extensible and applicable across various contexts
 * (users, companies, orders, etc.).</p>
 *
 * <h4>Usage:</h4>
 * <ul>
 *   <li>Used in {@code AddressLink} and its subclasses to qualify the address purpose</li>
 *   <li>Allows filtering and retrieving addresses by type</li>
 *   <li>Supports business logic for address selection (e.g., default billing address)</li>
 * </ul>
 *
 * @see be.steby.CoreProject.dl.entities.address.AddressLink
 */
public enum AddressType {

    /**
     * Primary/main address.
     * Typically the default address used when no specific type is required.
     */
    PRIMARY,

    /**
     * Billing address.
     * Used for invoicing and payment-related communications.
     */
    BILLING,

    /**
     * Shipping/delivery address.
     * Used for physical goods delivery.
     */
    SHIPPING,

    /**
     * Residential address.
     * The person's home or living address.
     */
    RESIDENTIAL,

    /**
     * Professional/work address.
     * Office or workplace address.
     */
    PROFESSIONAL,

    /**
     * Temporary address.
     * Short-term or seasonal address (e.g., vacation home, temporary relocation).
     */
    TEMPORARY,

    /**
     * Legal/registered address.
     * Official address for legal or administrative purposes (e.g., company registration).
     */
    LEGAL,

    /**
     * Warehouse address.
     * Storage or distribution center address (typically for companies).
     */
    WAREHOUSE,

    /**
     * Return address.
     * Address used for product returns or correspondence replies.
     */
    RETURN,

    /**
     * Other/unspecified type.
     * Catch-all for addresses that don't fit other categories.
     */
    OTHER
}