package be.steby.CoreProject.bll.events.device;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;


/**
 * Événement émis lorsque le niveau de confiance d'un appareil est modifié.
 * Cet événement est publié après la mise à jour dans la base de données.
 */
public record DeviceTrustLevelChangedEvent (
    Long deviceId,
    DeviceTrustLevel newTrustLevel,
    String oldLevel,
    User user,
    Device device,
    RequestContext request){}


