package be.steby.CoreProject.bll.services;

import be.steby.CoreProject.bll.models.RequestContext;
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

    Device detectFromRequestContext(RequestContext requestContext, User user);

    void updateTrustLevel(Long deviceId, DeviceTrustLevel level, HttpServletRequest request);

    void saveDevice(Device device);

    Device confirmDevice(String token);

    void rejectDevice(String token);

    Long getTotalDevices();

    void requestConfirmationLink(HttpServletRequest request);

    void disconnectDevice(Long deviceId, HttpServletRequest request);

    void disconnectAllOtherDevices(HttpServletRequest request);
}
