package be.steby.CoreProject.bll.domains.device.events;

import be.steby.CoreProject.dl.entities.Device;

public record DevicePersistedEvent(
        Device device
) {
}
