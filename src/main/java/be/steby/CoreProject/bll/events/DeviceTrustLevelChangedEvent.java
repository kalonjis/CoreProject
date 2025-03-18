package be.steby.CoreProject.bll.events;

import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import lombok.Getter;

@Getter
public class DeviceTrustLevelChangedEvent {
    private final Long deviceId;
    private final DeviceTrustLevel newTrustLevel;

    public DeviceTrustLevelChangedEvent(Long deviceId, DeviceTrustLevel newTrustLevel) {
        this.deviceId = deviceId;
        this.newTrustLevel = newTrustLevel;
    }
}
