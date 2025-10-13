package be.steby.CoreProject.bll.domains.admin.services.device;

import be.steby.CoreProject.bll.domains.admin.services.permissions.AdminPermissionValidator;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDeviceServiceImpl implements AdminDeviceService {

    private final DeviceService deviceService;
    private final UserService userService;
    private final AdminPermissionValidator adminPermissionValidator;

    @Override
    public List<Device> getUserDevices(String publicUserId) {
        User target = userService.getUserByPublicId(publicUserId);
        User admin = userService.getAuthenticatedUser();

        adminPermissionValidator.validateAdminActionOnAllUsers(admin, target, true, "get-user's-all-devices ");

        List<Device> devices = deviceService.getUserDevices(target);
        return devices;
    }


    @Override
    public Device getDeviceByPublicId(String devicePublicId) {
        Device device = deviceService.getDeviceByPublicId(devicePublicId);
        User deviceOwner = device.getUser();
        User admin = userService.getAuthenticatedUser();

        adminPermissionValidator.validateAdminActionOnAllUsers(admin, deviceOwner, true, "get-device-by-publicId");

        return device;
    }
}
