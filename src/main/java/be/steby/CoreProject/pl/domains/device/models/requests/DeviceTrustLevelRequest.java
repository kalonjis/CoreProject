package be.steby.CoreProject.pl.domains.device.models.requests;

import be.steby.CoreProject.dl.enums.DeviceTrustLevel;

public record DeviceTrustLevelRequest(
        DeviceTrustLevel deviceTrustLevel
) {
}