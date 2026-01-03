package be.steby.CoreProject.dl.enums;

/**
 * Enumeration defining the modification strategies for shared addresses.
 *
 * <p>Since an {@code Address} can be linked to multiple entities (users, companies, orders),
 * this enum determines how modifications to an address should be handled to maintain
 * data integrity and meet various business requirements.</p>
 *
 * <h4>Usage:</h4>
 * <ul>
 *   <li>Can be configured at application level (default behavior)</li>
 *   <li>Can be overridden per entity type or per address link</li>
 *   <li>Used by the address service layer to determine modification behavior</li>
 * </ul>
 *
 * <h4>Example scenarios:</h4>
 * <pre>
 * - SHARED: A family sharing the same home address; changes apply to all members
 * - COPY_ON_WRITE: User modifies a previously shared address; gets their own copy
 * - OWNER_ONLY: Only the original creator can modify; others must create new
 * - IMMUTABLE: Order addresses frozen at purchase time; never modified
 * </pre>
 *
 * @see be.steby.CoreProject.dl.entities.address.Address
 * @see be.steby.CoreProject.dl.entities.address.AddressLink
 */
public enum AddressModificationStrategy {

    /**
     * Shared modification strategy.
     *
     * <p>Modifications to the address are applied globally and affect all entities
     * linked to this address.</p>
     *
     * <p><b>Use case:</b> Family members or roommates sharing the same physical address
     * where changes (e.g., apartment number correction) should apply to everyone.</p>
     *
     * <p><b>Warning:</b> This can have unintended side effects if users are unaware
     * that others share the same address record.</p>
     */
    SHARED,

    /**
     * Copy-on-write modification strategy.
     *
     * <p>When a modification is requested, the system automatically creates a copy
     * of the address for the requesting entity, leaving the original unchanged
     * for other linked entities.</p>
     *
     * <p><b>Use case:</b> Default safe behavior for most applications.
     * Prevents accidental modifications affecting others while allowing flexibility.</p>
     *
     * <p><b>Implementation note:</b> The service layer should:
     * <ol>
     *   <li>Detect if the address is shared (linked to multiple entities)</li>
     *   <li>If shared, create a new Address with modified data</li>
     *   <li>Update the AddressLink to point to the new Address</li>
     *   <li>Optionally clean up orphaned addresses</li>
     * </ol>
     * </p>
     */
    COPY_ON_WRITE,

    /**
     * Owner-only modification strategy.
     *
     * <p>Only the original creator (determined by {@code createdBy} field) of the address
     * can modify it. Other linked entities must create their own address if they need
     * different data.</p>
     *
     * <p><b>Use case:</b> Scenarios where address ownership matters, such as
     * a primary account holder managing family addresses.</p>
     *
     * <p><b>Behavior for non-owners:</b> Modification attempts should either:
     * <ul>
     *   <li>Be rejected with an appropriate error</li>
     *   <li>Automatically trigger COPY_ON_WRITE behavior (configurable)</li>
     * </ul>
     * </p>
     */
    OWNER_ONLY,

    /**
     * Immutable address strategy.
     *
     * <p>The address cannot be modified once created. Any "modification" requires
     * creating a new address and updating the link.</p>
     *
     * <p><b>Use case:</b> Order addresses that must be preserved exactly as they were
     * at the time of purchase for legal, auditing, or shipping accuracy purposes.</p>
     *
     * <p><b>Implementation note:</b> Consider using a snapshot/copy of the address
     * at link creation time rather than referencing a shared Address entity.</p>
     */
    IMMUTABLE,

    /**
     * Versioned modification strategy.
     *
     * <p>Modifications create a new version of the address while preserving
     * the history. Previous versions remain accessible for auditing.</p>
     *
     * <p><b>Use case:</b> Applications requiring full address history tracking,
     * such as financial services or legal compliance systems.</p>
     *
     * <p><b>Implementation note:</b> Requires additional versioning infrastructure
     * (version number, previous version reference, or separate history table).</p>
     */
    VERSIONED
}