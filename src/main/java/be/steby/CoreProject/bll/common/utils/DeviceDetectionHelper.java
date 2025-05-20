package be.steby.CoreProject.bll.common.utils;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Classe utilitaire qui fournit des méthodes communes pour la détection d'appareils
 * et l'exécution d'actions avec ces appareils.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceDetectionHelper {

    private final DeviceService deviceService;

    /**
     * Exécute une action après avoir tenté de détecter le device.
     * Si la détection échoue, l'action est quand même exécutée avec un device null.
     *
     * @param user L'utilisateur concerné
     * @param requestContext Le contexte de la requête pour la détection du device
     * @param action L'action à exécuter avec le device (peut être null)
     */
    public void executeWithDeviceDetection(User user,
                                           RequestContext requestContext,
                                           Consumer<Device> action) {
        Device device = null;

        try {
            device = deviceService.detectFromRequestContext(requestContext, user);
        } catch (Exception e) {
            log.warn("Impossible de détecter le device pour l'utilisateur {} : {}",
                    user.getUsername(), e.getMessage());
            // On continue avec un device null
        }

        // Exécute l'action avec le device (qui peut être null)
        action.accept(device);
    }
}