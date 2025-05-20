package be.steby.CoreProject.bll.events.device;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

public record DeviceDetectedEvent(
        Device device,
        User user,
        boolean isNewDevice,
        boolean isBlacklisted,
        boolean isConfirmed,
        boolean isFirstDevice,
        RequestContext request
) {}