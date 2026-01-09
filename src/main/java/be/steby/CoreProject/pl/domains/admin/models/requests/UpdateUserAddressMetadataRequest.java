package be.steby.CoreProject.pl.domains.admin.models.requests;

import be.steby.CoreProject.dl.enums.AddressType;

/**
 * Request DTO for updating user address link metadata by admin.
 *
 * <p>All fields are optional (nullable), allowing partial updates.
 * Only provided fields will be updated; null fields are ignored.</p>
 *
 * <h4>Important notes:</h4>
 * <ul>
 *   <li>Empty strings ("") for label/notes will clear the field</li>
 *   <li>Setting isPrimary=true will unset other addresses' isPrimary</li>
 *   <li>Setting isDefault=true will unset other addresses' isDefault for the same type</li>
 *   <li>Changing addressType may affect isDefault behavior</li>
 * </ul>
 *
 * <h4>Example usage:</h4>
 * <pre>
 * // Update only label and isPrimary
 * {
 *   "label": "New Home",
 *   "isPrimary": true
 * }
 *
 * // Clear notes and change type
 * {
 *   "notes": "",
 *   "addressType": "BILLING"
 * }
 * </pre>
 *
 * @param label            User-defined label (null = no change, "" = clear)
 * @param notes            Optional notes (null = no change, "" = clear)
 * @param addressType      Type of address (RESIDENTIAL, BILLING, etc.)
 * @param isPrimary        Whether this is the user's primary address
 * @param isDefault        Whether this is the default for its type
 * @param billingEligible  Whether eligible for billing
 * @param shippingEligible Whether eligible for shipping
 * @param verifiedByOwner  Whether verified by the user
 * @param setDefaultOnTypeChange Whether to set as default when changing type (default: false)
 */
public record UpdateUserAddressMetadataRequest(
        String label,
        String notes,
        AddressType addressType,
        Boolean isPrimary,
        Boolean isDefault,
        Boolean billingEligible,
        Boolean shippingEligible,
        Boolean verifiedByOwner,
        Boolean setDefaultOnTypeChange
) {
    /**
     * Checks if any field is provided for update.
     *
     * @return true if at least one field is non-null
     */
    public boolean hasAnyUpdate() {
        return label != null
                || notes != null
                || addressType != null
                || isPrimary != null
                || isDefault != null
                || billingEligible != null
                || shippingEligible != null
                || verifiedByOwner != null;
    }

    /**
     * Checks if label or notes need updating.
     */
    public boolean hasMetadataUpdate() {
        return label != null || notes != null;
    }

    /**
     * Checks if eligibility flags need updating.
     */
    public boolean hasEligibilityUpdate() {
        return billingEligible != null || shippingEligible != null;
    }

    /**
     * Returns the value to use for setDefaultOnTypeChange.
     * Defaults to false if not specified.
     */
    public boolean shouldSetDefaultOnTypeChange() {
        return setDefaultOnTypeChange != null && setDefaultOnTypeChange;
    }
}