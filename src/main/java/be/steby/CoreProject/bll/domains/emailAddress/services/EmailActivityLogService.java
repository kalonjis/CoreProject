package be.steby.CoreProject.bll.domains.email.services;

import be.steby.CoreProject.bll.common.logs.AbstractActivityLogService;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.actionLogTypes.EmailAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Email domain activity logging service - KISS VERSION
 * Uses ONLY EmailAction enum values
 */
@Service
@Slf4j
public class EmailActivityLogService extends AbstractActivityLogService {

    public EmailActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "EMAIL";
    }

    // ================== EMAIL DOMAIN METHODS ONLY ==================

    @Transactional
    public ActivityLog logEmailChangeRequested(User user, String oldEmail, String newEmail, Device device, RequestContext context) {
        ActivityLog log = logSuccess(user, device, EmailAction.EMAIL_CHANGE_REQUESTED, context);
        log.setActionDetails("From: " + oldEmail + " To: " + newEmail);
        return activityLogRepository.save(log);
    }

    @Transactional
    public ActivityLog logEmailChangeConfirmed(User user, String newEmail, Device device, RequestContext context) {
        ActivityLog log = logSuccess(user, device, EmailAction.EMAIL_CHANGE_CONFIRMED, context);
        log.setActionDetails("New email confirmed: " + newEmail);
        return activityLogRepository.save(log);
    }

    @Transactional
    public ActivityLog logEmailVerificationSent(User user, Device device, RequestContext context) {
        return logSuccess(user, device, EmailAction.EMAIL_VERIFICATION_SENT, context);
    }

    @Transactional
    public ActivityLog logEmailVerified(User user, Device device, RequestContext context) {
        return logSuccess(user, device, EmailAction.EMAIL_VERIFIED, context);
    }
}
