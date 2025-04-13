package be.steby.CoreProject.bll.services;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface DeviceService {

    Device getDeviceById(Long id);

    Device getMyDevice(Long deviceId);

    List<Device> getMyDeviceList();

    List<Device> getUserDevice(User user);

    Device detectAndRegisterDevice(HttpServletRequest request, User user, boolean confirmDevice);

    Device detectCurrentDevice(HttpServletRequest request);

    void updateTrustLevel(Long deviceId, DeviceTrustLevel level);

    //Device revokeDeviceTrust(Long deviceId);

    void saveDevice(Device device);

    Device confirmDevice(String token);

    void rejectDevice(String token);

    Long getTotalDevices();
}
