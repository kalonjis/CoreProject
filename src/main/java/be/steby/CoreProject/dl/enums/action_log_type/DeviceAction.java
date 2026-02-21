package be.steby.CoreProject.dl.enums.action_log_type;

/**
 * Device domain actions.
 *
 * <p>Each constant maps 1-to-1 to a device domain event:
 * <ul>
 *   <li>{@code DEVICE_CONFIRMED}                   ← DeviceConfirmedEvent</li>
 *   <li>{@code DEVICE_REJECTED}                    ← DeviceRejectedEvent</li>
 *   <li>{@code DEVICE_TRUST_LEVEL_UPDATED}         ← DeviceTrustLevelUpdatedEvent</li>
 *   <li>{@code DEVICE_DISCONNECTED}                ← DeviceDisconnectedEvent</li>
 *   <li>{@code DEVICE_ALL_OTHERS_DISCONNECTED}     ← DeviceAllOthersDisconnectedEvent</li>
 *   <li>{@code DEVICE_CONFIRMATION_LINK_REQUESTED} ← DeviceConfirmationLinkRequestedEvent</li>
 * </ul>
 */
public enum DeviceAction implements ActionLogType {

    // =========================================================================
    // Confirmation
    // =========================================================================

    DEVICE_CONFIRMED("Device confirmed via email link — trust level set to TRUSTED"),
    DEVICE_REJECTED("Device rejected via email link — device blacklisted"),

    // =========================================================================
    // Trust level
    // =========================================================================

    DEVICE_TRUST_LEVEL_UPDATED("Device trust level updated by user"),

    // =========================================================================
    // Disconnection
    // =========================================================================

    DEVICE_DISCONNECTED("Device remotely disconnected by user"),
    DEVICE_ALL_OTHERS_DISCONNECTED("All other devices disconnected by user"),

    // =========================================================================
    // Confirmation link
    // =========================================================================

    DEVICE_CONFIRMATION_LINK_REQUESTED("New device confirmation link requested");

    // =========================================================================

    private final String description;

    DeviceAction(String description) {
        this.description = description;
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return "DEVICE";
    }
}