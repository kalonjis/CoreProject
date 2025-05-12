package be.steby.CoreProject.bll.events.device;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import jakarta.servlet.http.HttpServletRequest;

public record DeviceDetectedEvent(
        Device device,
        User user,
        boolean isNewDevice,
        boolean isBlacklisted,
        boolean isConfirmed,
        boolean isFirstDevice,
        HttpServletRequest request
) {}