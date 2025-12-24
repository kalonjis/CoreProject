package be.steby.CoreProject.bll.domains.admin.services.device;

import be.steby.CoreProject.dl.entities.Device;
import java.util.List;
import java.util.Map;

/**
 * Service interface for administrative device management operations.
 * Handles device-related administrative tasks with proper permission validation.
 *
 * All methods require the authenticated user to have ADMIN privileges.
 * Permission validation is performed within each method implementation.
 */
public interface AdminDeviceService {

    /**
     * Retrieves all devices associated with a specific user.
     *
     * @param publicUserId User's public ID
     * @return List of devices belonging to the user
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException
     *         if user doesn't exist
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException
     *         if actor lacks admin privileges
     */
    List<Device> getUserDevices(String publicUserId);

    /**
     * Retrieves a specific device by its public ID.
     *
     * @param devicePublicId Device's public ID
     * @return Device entity
     * @throws be.steby.CoreProject.bll.domains.device.exceptions.DeviceNotFoundException
     *         if device doesn't exist
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException
     *         if actor lacks admin privileges
     */
    Device getDeviceByPublicId(String devicePublicId);

    /**
     * Retrieves comprehensive device statistics for administrative dashboards.
     *
     * Provides multiple metrics in a single call for efficiency:
     * - totalDevices: Total number of devices in the system
     * - trustedDevices: Number of devices with TRUSTED trust level
     * - untrustedDevices: Number of devices with UNTRUSTED trust level
     * - activeDevices: Devices with activity in the last 30 days
     * - inactiveDevices: Devices without recent activity
     *
     * All statistics are computed atomically within a read-only transaction
     * to ensure data consistency.
     *
     * @return Map containing device statistics (timestamp added by controller)
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException
     *         if actor lacks admin privileges
     */
    Map<String, Object> getDeviceStatistics();
}