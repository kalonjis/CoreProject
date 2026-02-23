package be.steby.CoreProject.bll.domains.device.services;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import be.steby.CoreProject.bll.common.exceptions.OwnershipException;
import be.steby.CoreProject.bll.domains.device.exceptions.DeviceNotFoundException;

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
     * @param user The user for whom the device is being detected
     * @return The detected/registered device
     */
    Device detectAndRegisterDevice(User user);

    Device detectCurrentDevice();

    void updateTrustLevel(String publicId, DeviceTrustLevel level);

    void saveDevice(Device device);

    Device confirmDevice(String token);

    void rejectDevice(String token);

    Long getTotalDevices();

    void requestConfirmationLink();

    void disconnectDevice(String publicId);

    int disconnectAllOtherDevices();

    //int disconnectAllDevicesExceptCurrent(User user, Long currentDeviceId);

    int disconnectAllDevicesForUser(User user);
}