package be.steby.CoreProject.bll.domains.crm.telephony.sip.services;

import be.steby.CoreProject.bll.domains.crm.telephony.sip.models.SaveSipConfigRequest;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CommercialSipConfig;

/**
 * Manages SIP extension credentials per commercial.
 *
 * <p>An admin assigns a SIP username and password to each commercial who will
 * place calls via the SIP adapter. The frontend reads these credentials on
 * CRM load to initialise SIP.js against the Asterisk WebSocket.</p>
 */
public interface CommercialSipConfigService {

    /**
     * Returns the SIP config for the given commercial.
     *
     * @param actor the authenticated commercial requesting their own credentials
     * @throws be.steby.CoreProject.bll.domains.crm.telephony.sip.exceptions.SipConfigNotFoundException
     *         if no config is assigned to that user
     */
    CommercialSipConfig getForActor(User actor);

    /**
     * Returns the SIP config by its public UUID (admin use).
     */
    CommercialSipConfig getByPublicId(String publicId);

    /**
     * Creates a SIP config for a commercial (admin only — one per user).
     *
     * @throws be.steby.CoreProject.bll.domains.crm.telephony.sip.exceptions.SipConfigValidationException
     *         if the user already has a config or the SIP username is already taken
     */
    CommercialSipConfig create(SaveSipConfigRequest request);

    /**
     * Updates an existing SIP config.
     *
     * @throws be.steby.CoreProject.bll.domains.crm.telephony.sip.exceptions.SipConfigNotFoundException
     *         if the config does not exist
     * @throws be.steby.CoreProject.bll.domains.crm.telephony.sip.exceptions.SipConfigValidationException
     *         if the new SIP username conflicts with another user
     */
    CommercialSipConfig update(String publicId, SaveSipConfigRequest request);

    /**
     * Deletes a SIP config. The commercial will no longer be able to place SIP calls.
     */
    void delete(String publicId);
}
