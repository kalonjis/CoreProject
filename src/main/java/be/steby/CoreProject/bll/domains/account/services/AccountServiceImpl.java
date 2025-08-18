package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.bll.domains.account.events.*;
import be.steby.CoreProject.bll.domains.account.exceptions.AccountAlreadyActivatedException;
import be.steby.CoreProject.bll.domains.account.exceptions.deactivation.AccountAlreadyDeactivatedException;
import be.steby.CoreProject.bll.domains.account.exceptions.deactivation.InvalidDeactivationRequestException;
import be.steby.CoreProject.bll.domains.account.exceptions.reactivation.ReactivationNotAllowedException;
import be.steby.CoreProject.bll.domains.account.models.DeactivationRequest;
import be.steby.CoreProject.bll.domains.account.models.DeactivationValidationResult;
import be.steby.CoreProject.bll.domains.account.services.tokens.deactivation.AccountDeactivationAttemptServiceImpl;
import be.steby.CoreProject.bll.domains.account.services.tokens.deactivation.AccountDeactivationTokenServiceImpl;
import be.steby.CoreProject.bll.domains.account.services.tokens.reactivation.AccountReactivationAttemptServiceImpl;
import be.steby.CoreProject.bll.domains.account.services.tokens.reactivation.AccountReactivationTokenServiceImpl;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.services.context.RequestContextService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.account.services.tokens.confirmation.AccountConfirmationAttemptServiceImpl;
import be.steby.CoreProject.bll.domains.account.services.tokens.confirmation.AccountConfirmationTokenServiceImpl;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;
import be.steby.CoreProject.dl.entities.tokens.AccountDeactivationToken;
import be.steby.CoreProject.dl.entities.tokens.AccountReactivationToken;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountConfirmationTokenServiceImpl accountConfirmationTokenService;
    private final AccountConfirmationAttemptServiceImpl accountConfirmationAttemptService;

    private final AccountDeactivationTokenServiceImpl accountDeactivationTokenService;
    private final AccountDeactivationAttemptServiceImpl accountDeactivationAttemptService;
    private final DeactivationPolicyService deactivationPolicyService;

    private final AccountReactivationTokenServiceImpl accountReactivationTokenService;

    private final UserService userService;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final RequestContextService requestContextService;
    private final ApplicationEventPublisher eventPublisher;


    @Override
    public User confirmNewUserAccount(String token, HttpServletRequest request) {
        AccountConfirmationToken accountConfirmationToken = accountConfirmationTokenService.getToken(token);
        accountConfirmationTokenService.verifyTokenValidity(accountConfirmationToken);
        User user = accountConfirmationToken.getUser();

        if (user.isEnabled()) {
            throw new AccountAlreadyActivatedException("The user with email address " + user.getEmail() + " is already activated!");
        }

        userService.activateUser(user.getId());
        accountConfirmationTokenService.revokeAllUserTokens(user);
        accountConfirmationAttemptService.clearAttempts(user);

        RequestContext requestContext = requestContextService.captureRequestContext(request);
        AccountConfirmationEvent event = new AccountConfirmationEvent(user, requestContext);
        eventPublisher.publishEvent(event);

        return user;
    }

    /**
     * ✅ FIXED - Method signature matches interface: requestActivation
     */
    @Override
    public void requestActivation(String token, HttpServletRequest request) {
        // Get user from token to create new confirmation token
        AccountConfirmationToken oldToken = accountConfirmationTokenService.getToken(token);
        User user = oldToken.getUser();

        if (user.isEnabled()) {
            throw new AccountAlreadyActivatedException("The user with email address " + user.getEmail() + " is already activated!");
        }

        AccountConfirmationToken accountConfirmationToken = accountConfirmationTokenService.createAccountConfirmationToken(user);
        RequestContext requestContext = requestContextService.captureRequestContext(request);

        RequestAccountActivationEvent event = new RequestAccountActivationEvent(user, accountConfirmationToken.getToken(), requestContext);
        eventPublisher.publishEvent(event);
    }

    /**
     * ✅ FIXED - Parameter order matches interface: (User user, DeactivationRequest deactivationRequest, HttpServletRequest request)
     */
    @Override
    public void requestDeactivation(User user, DeactivationRequest deactivationRequest, HttpServletRequest request) {
        if (!user.isEnabled()) {
            throw new AccountAlreadyDeactivatedException("The user with email address " + user.getEmail() + " is already deactivated!");
        }

        DeactivationValidationResult validationResult = deactivationPolicyService.validateDeactivationRequest(deactivationRequest, user);
        if (!validationResult.isValid()) {
            throw new InvalidDeactivationRequestException("Validation failed: " + String.join(", ", validationResult.errors()));
        }

        AccountDeactivationToken accountDeactivationToken = accountDeactivationTokenService.createAccountDeactivationToken(
                user, deactivationRequest.deactivationReason(), deactivationRequest.reasonDetails());

        RequestContext requestContext = requestContextService.captureRequestContext(request);
        RequestAccountDeactivationEvent event = new RequestAccountDeactivationEvent(
                user, accountDeactivationToken.getToken(),
                deactivationRequest.deactivationReason(),
                deactivationRequest.reasonDetails(),
                requestContext);

        eventPublisher.publishEvent(event);
    }

    /**
     * ✅ INTERFACE METHOD - deactivateAccount
     */
    @Override
    public User deactivateAccount(String token, HttpServletRequest httpRequest) {
        AccountDeactivationToken accountDeactivationToken = accountDeactivationTokenService.getToken(token);
        accountDeactivationTokenService.verifyTokenValidity(accountDeactivationToken);
        User user = accountDeactivationToken.getUser();

        if (!user.isEnabled()) {
            throw new AccountAlreadyDeactivatedException("The user with email address " + user.getEmail() + " is already deactivated!");
        }

        userService.deactivateUser(user.getId(), accountDeactivationToken.getDeactivationReason(), accountDeactivationToken.getReasonDetails());
        accountDeactivationTokenService.revokeAllUserTokens(user);
        refreshTokenService.revokeAllUserTokens(user);
        accountDeactivationAttemptService.clearAttempts(user);

        RequestContext requestContext = requestContextService.captureRequestContext(httpRequest);
        AccountDeactivationConfirmedEvent event = new AccountDeactivationConfirmedEvent(
                user,
                accountDeactivationToken.getDeactivationReason(),
                accountDeactivationToken.getReasonDetails(),
                requestContext
        );
        eventPublisher.publishEvent(event);

        return user;
    }

    /**
     * ✅ ULTRA-SIMPLIFIED - requestReactivation method (interface signature)
     */
    @Override
    public void requestReactivation(User user, HttpServletRequest httpRequest) {
        if (user.isEnabled()) {
            throw new AccountAlreadyActivatedException("The user with email address " + user.getEmail() + " is already activated!");
        }

        // ✅ SIMPLIFIED LOGIC with direct enum field access
        boolean canReactivate = determineReactivationEligibility(user);

        if (!canReactivate) {
            String reason = getReactivationDenialReason(user);
            throw new ReactivationNotAllowedException(reason);
        }

        // ✅ Log reactivation request with context
        logReactivationAttempt(user, true);

        AccountReactivationToken accountReactivationToken = accountReactivationTokenService.createAccountReactivationToken(user);
        RequestContext requestContext = requestContextService.captureRequestContext(httpRequest);

        RequestAccountReactivationEvent event = new RequestAccountReactivationEvent(user, accountReactivationToken.getToken(), requestContext);
        eventPublisher.publishEvent(event);
    }

    /**
     * ✅ ULTRA-SIMPLIFIED - reactivateAccount method (interface signature)
     */
    @Override
    public User reactivateAccount(String token, HttpServletRequest httpRequest) {
        AccountReactivationToken accountReactivationToken = accountReactivationTokenService.getToken(token);
        accountReactivationTokenService.verifyTokenValidity(accountReactivationToken);
        User user = accountReactivationToken.getUser();

        if (user.isEnabled()) {
            throw new AccountAlreadyActivatedException("The user with email address " + user.getEmail() + " is already activated!");
        }

        // ✅ SAME SIMPLIFIED LOGIC
        boolean canReactivate = determineReactivationEligibility(user);

        if (!canReactivate) {
            String reason = getReactivationDenialReason(user);
            throw new ReactivationNotAllowedException(reason);
        }

        // ✅ Proceed with reactivation
        userService.reactivateUser(user);
        accountReactivationTokenService.revokeAllUserTokens(user);
        accountDeactivationAttemptService.clearAttempts(user);

        // ✅ Log successful reactivation with context
        logReactivationAttempt(user, false);

        RequestContext requestContext = requestContextService.captureRequestContext(httpRequest);
        AccountReactivationConfirmedEvent event = new AccountReactivationConfirmedEvent(user, requestContext);
        eventPublisher.publishEvent(event);

        return user;
    }

    // ================================================================================
    // ✅ PRIVATE HELPER METHODS - Ultra-simple logic
    // ================================================================================

    /**
     * ✅ MAIN LOGIC - Direct enum field access, no service calls!
     */
    private boolean determineReactivationEligibility(User user) {
        if (user.isAdminDeactivated()) {
            // ✅ Direct enum field access - no switch statement!
            return user.getAdminDeactivationReason().allowsReactivation();

        } else if (user.isSelfDeactivated()) {
            // ✅ Direct enum field access - no service dependency!
            return user.getDeactivationReason().allowsReactivation();

        } else {
            log.error("User {} is disabled but has no clear deactivation reason (enabled=false, isAdminDeactivated=false, isSelfDeactivated=false)",
                    user.getEmail());
            return false;
        }
    }

    /**
     * ✅ ERROR MESSAGE HELPER - Contextual messages based on deactivation type
     */
    private String getReactivationDenialReason(User user) {
        if (user.isAdminDeactivated()) {
            return String.format(
                    "Account reactivation is not allowed for admin deactivation category: %s. Contact an administrator for manual review.",
                    user.getAdminDeactivationReason().getDisplayName()
            );

        } else if (user.isSelfDeactivated()) {
            return String.format(
                    "Account reactivation is not allowed for this deactivation reason: %s. %s",
                    user.getDeactivationReason().getDisplayName(),
                    user.getDeactivationReason() == DeactivationReason.GDPR_REQUEST
                            ? "This action is permanent and cannot be reversed."
                            : "Please contact support for assistance."
            );

        } else {
            return "Account deactivation reason unclear. Please contact support.";
        }
    }

    /**
     * ✅ AUDIT LOGGING - Enhanced logging with deactivation context
     */
    private void logReactivationAttempt(User user, boolean isRequest) {
        String action = isRequest ? "requested" : "completed";

        if (user.isAdminDeactivated()) {
            AdminDeactivationCategory category = user.getAdminDeactivationReason();
            String adminInfo = user.getAdminDeactivatedBy() != null ?
                    user.getAdminDeactivatedBy().getUsername() : "unknown";

            log.info("Reactivation {} for admin-deactivated user {} (category: {}, allows_reactivation: {}, deactivated_by: {})",
                    action, user.getEmail(), category.getDisplayName(), category.allowsReactivation(), adminInfo);

        } else if (user.isSelfDeactivated()) {
            DeactivationReason reason = user.getDeactivationReason();

            log.info("Reactivation {} for self-deactivated user {} (reason: {}, allows_reactivation: {})",
                    action, user.getEmail(), reason.getDisplayName(), reason.allowsReactivation());

        } else {
            log.warn("Reactivation {} for user {} with unclear deactivation status", action, user.getEmail());
        }
    }

    // ================================================================================
    // ✅ PUBLIC UTILITY METHODS - For use by controllers, admin services, etc.
    // ================================================================================

    /**
     * ✅ PUBLIC UTILITY METHOD - For use by controllers, admin services, etc.
     */
    public boolean canUserRequestReactivation(User user) {
        if (user == null || user.isEnabled()) {
            return false;
        }

        return determineReactivationEligibility(user);
    }

    /**
     * ✅ PUBLIC UTILITY METHOD - Get user-friendly reactivation message
     */
    public String getReactivationStatusMessage(User user) {
        if (user == null) {
            return "User not found.";
        }

        if (user.isEnabled()) {
            return "Account is already active.";
        }

        boolean canReactivate = determineReactivationEligibility(user);

        if (canReactivate) {
            if (user.isAdminDeactivated()) {
                return String.format("Your account was deactivated by an administrator (%s). You can request reactivation.",
                        user.getAdminDeactivationReason().getDisplayName());
            } else {
                return "You can reactivate your account at any time.";
            }
        } else {
            if (user.isAdminDeactivated()) {
                return String.format("Your account was permanently deactivated (%s). Please contact support.",
                        user.getAdminDeactivationReason().getDisplayName());
            } else if (user.isSelfDeactivated() && user.getDeactivationReason() == DeactivationReason.GDPR_REQUEST) {
                return "Your account was permanently deleted under GDPR. This action cannot be reversed.";
            } else {
                return "Account reactivation is not available. Please contact support.";
            }
        }
    }
}