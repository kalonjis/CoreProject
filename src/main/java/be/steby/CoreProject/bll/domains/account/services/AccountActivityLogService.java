package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.bll.common.logs.AbstractActivityLogService;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.actionLogTypes.AccountAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account management domain activity logging service - KISS VERSION
 * Uses ONLY AccountAction enum values
 */
@Service
@Slf4j
public class AccountActivityLogService extends AbstractActivityLogService {

    public AccountActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "ACCOUNT";
    }

    // ================== ACCOUNT DOMAIN METHODS ONLY ==================

    @Transactional
    public ActivityLog logAccountCreated(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.ACCOUNT_CREATED, context);
    }

    @Transactional
    public ActivityLog logAccountActivated(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.ACCOUNT_ACTIVATED, context);
    }

    @Transactional
    public ActivityLog logAccountDeactivated(User user, String reason, Device device, RequestContext context) {
        ActivityLog log = logSuccess(user, device, AccountAction.ACCOUNT_DEACTIVATED, context);
        log.setActionDetails("Deactivation reason: " + reason);
        return activityLogRepository.save(log);
    }

    @Transactional
    public ActivityLog logAccountDeleted(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.ACCOUNT_DELETED, context);
    }

    @Transactional
    public ActivityLog logTwoFactorEnabled(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.TWO_FACTOR_ENABLED, context);
    }

    @Transactional
    public ActivityLog logTwoFactorDisabled(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.TWO_FACTOR_DISABLED, context);
    }

    @Transactional
    public ActivityLog logDataDeletionRequested(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.DATA_DELETION_REQUESTED, context);
    }
}