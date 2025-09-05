package be.steby.CoreProject.bll.domains.device.events;

import be.steby.CoreProject.dl.entities.Device;
import lombok.Value;

/**
 * Domain event fired when a device is created or updated.
 * This event is used to maintain cache consistency across the application.
 */
@Value
public class DeviceCreatedOrUpdatedEvent {
    Device device;
    String action; // "CREATED" or "UPDATED"

    public static DeviceCreatedOrUpdatedEvent created(Device device) {
        return new DeviceCreatedOrUpdatedEvent(device, "CREATED");
    }

    public static DeviceCreatedOrUpdatedEvent updated(Device device) {
        return new DeviceCreatedOrUpdatedEvent(device, "UPDATED");
    }
}