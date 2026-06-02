package be.steby.CoreProject.bll.domains.crm.telephony.sip.services;

import be.steby.CoreProject.bll.domains.crm.telephony.sip.exceptions.SipConfigNotFoundException;
import be.steby.CoreProject.bll.domains.crm.telephony.sip.exceptions.SipConfigValidationException;
import be.steby.CoreProject.bll.domains.crm.telephony.sip.models.SaveSipConfigRequest;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dal.repositories.crm.CommercialSipConfigRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CommercialSipConfig;
import be.steby.CoreProject.il.telephony.crypto.TelephonyCredentialsEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommercialSipConfigServiceImpl implements CommercialSipConfigService {

    private final CommercialSipConfigRepository    repository;
    private final UserService                      userService;
    private final TelephonyCredentialsEncryptionService encryptionService;

    @Override
    public CommercialSipConfig getForActor(User actor) {
        return repository.findByUserId(actor.getId())
                .orElseThrow(() -> SipConfigNotFoundException.forUser(actor.getPublicId()));
    }

    @Override
    public CommercialSipConfig getByPublicId(String publicId) {
        return repository.findByPublicId(publicId)
                .orElseThrow(() -> SipConfigNotFoundException.byPublicId(publicId));
    }

    @Override
    @Transactional
    public CommercialSipConfig create(SaveSipConfigRequest request) {
        User target = userService.getUserByPublicId(request.targetUserPublicId());

        if (repository.existsByUserId(target.getId())) {
            throw SipConfigValidationException.alreadyConfigured(target.getPublicId());
        }
        if (repository.existsBySipUsername(request.sipUsername())) {
            throw SipConfigValidationException.usernameAlreadyTaken(request.sipUsername());
        }

        CommercialSipConfig config = CommercialSipConfig.builder()
                .user(target)
                .sipUsername(request.sipUsername())
                .encryptedSipPassword(encryptionService.encrypt(request.sipPassword()))
                .displayName(request.displayName())
                .build();

        CommercialSipConfig saved = repository.save(config);
        log.info("SIP config created — publicId: {}, user: {}, sipUsername: {}",
                saved.getPublicId(), target.getId(), request.sipUsername());
        return saved;
    }

    @Override
    @Transactional
    public CommercialSipConfig update(String publicId, SaveSipConfigRequest request) {
        CommercialSipConfig config = getByPublicId(publicId);

        if (!config.getSipUsername().equals(request.sipUsername())
                && repository.existsBySipUsername(request.sipUsername())) {
            throw SipConfigValidationException.usernameAlreadyTaken(request.sipUsername());
        }

        config.setSipUsername(request.sipUsername());
        config.setEncryptedSipPassword(encryptionService.encrypt(request.sipPassword()));
        config.setDisplayName(request.displayName());

        CommercialSipConfig saved = repository.save(config);
        log.info("SIP config updated — publicId: {}", publicId);
        return saved;
    }

    @Override
    @Transactional
    public void delete(String publicId) {
        CommercialSipConfig config = getByPublicId(publicId);
        repository.delete(config);
        log.info("SIP config deleted — publicId: {}", publicId);
    }
}
