package be.steby.CoreProject.bll.domains.device.services;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface DeviceService {

    Device getDeviceById(Long id);

    Device getDeviceByPublicId(String publicId);

    Device getMyDevice(Long deviceId);

    /**
     * Retrieves a device by its public ID with ownership validation.
     * Only the authenticated user's own devices can be retrieved.
     *
     * @param publicId The device public ID
     * @return The device if owned by the authenticated user
     * @throws DeviceNotFoundException if device not found
     * @throws OwnershipException if user doesn't own the device
     */
    Device getMyDeviceByPublicId(String publicId);

    List<Device> getMyDeviceList();

    List<Device> getUserDevices(User user);

    /**
     * Detects and registers a device based on the HTTP request.
     * @param request The HTTP request containing device information
     * @param user The user for whom the device is being detected
     * @return The detected/registered device
     */
    Device detectAndRegisterDevice(HttpServletRequest request, User user);

    Device detectCurrentDevice(HttpServletRequest request);

    void updateTrustLevel(String publicId, DeviceTrustLevel level, HttpServletRequest request);

    void saveDevice(Device device);

    Device confirmDevice(String token);

    void rejectDevice(String token);

    Long getTotalDevices();

    void requestConfirmationLink(HttpServletRequest request);

    void disconnectDevice(String publicId, HttpServletRequest request);

    int disconnectAllOtherDevices(HttpServletRequest request);

    int disconnectAllDevicesExceptCurrent(User user, Long currentDeviceId);

    int disconnectAllDevicesForUser(User user);
}