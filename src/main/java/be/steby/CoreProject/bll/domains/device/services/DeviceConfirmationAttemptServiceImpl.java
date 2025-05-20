package be.steby.CoreProject.bll.domains.device.services;

import be.steby.CoreProject.dal.repositories.UserAttemptRepository;
import be.steby.CoreProject.dl.enums.AttemptType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class DeviceConfirmationAttemptServiceImpl extends DeviceAttemptTrackerServiceImpl {

    public DeviceConfirmationAttemptServiceImpl(
            UserAttemptRepository userAttemptRepository,
            @Value("${security.device-confirmation.max-attempts}") int maxAttempts,
            @Value("${security.device-confirmation.lockout-minutes}") int lockoutMinutes) {
        super(userAttemptRepository, maxAttempts, lockoutMinutes, AttemptType.DEVICE_CONFIRMATION);
    }


}
