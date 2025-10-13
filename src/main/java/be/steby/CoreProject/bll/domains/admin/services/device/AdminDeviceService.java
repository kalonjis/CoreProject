package be.steby.CoreProject.bll.domains.admin.services.device;

import be.steby.CoreProject.dl.entities.Device;

import java.util.List;

public interface AdminDeviceService {

    List<Device> getUserDevices(String publicUserId);

    Device getDeviceByPublicId(String devicePublicId);
}
