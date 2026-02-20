package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.bll.common.services.validation.email.EmailPolicyService;
import be.steby.CoreProject.bll.common.services.validation.password.PasswordPolicyService;
import be.steby.CoreProject.bll.domains.account.events.*;
import be.steby.CoreProject.bll.domains.account.exceptions.AccountAlreadyActivatedException;
import be.steby.CoreProject.bll.domains.account.exceptions.SignupValidationException;
import be.steby.CoreProject.bll.domains.account.exceptions.deactivation.AccountAlreadyDeactivatedException;
import be.steby.CoreProject.bll.domains.account.exceptions.deactivation.InvalidDeactivationRequestException;
import be.steby.CoreProject.bll.domains.account.exceptions.reactivation.ReactivationNotAllowedException;
import be.steby.CoreProject.bll.domains.account.models.*;
import be.steby.CoreProject.bll.domains.account.services.tokens.confirmation.AccountConfirmationAttemptServiceImpl;
import be.steby.CoreProject.bll.domains.account.services.tokens.confirmation.AccountConfirmationTokenServiceImpl;
import be.steby.CoreProject.bll.domains.account.services.tokens.deactivation.AccountDeactivationAttemptServiceImpl;
import be.steby.CoreProject.bll.domains.account.services.tokens.deactivation.AccountDeactivationTokenServiceImpl;
import be.steby.CoreProject.bll.domains.account.services.tokens.reactivation.AccountReactivationTokenServiceImpl;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.device.utils.DeviceContextProvider;
import be.steby.CoreProject.bll.domains.emailaddress.models.EmailValidationResult;
import be.steby.CoreProject.bll.domains.password.models.PasswordValidationResult;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.user.services.UsernameGeneratorService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;
import be.steby.CoreProject.dl.entities.tokens.AccountDeactivationToken;
import be.steby.CoreProject.dl.entities.tokens.AccountReactivationToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final UsernameGeneratorService usernameGeneratorService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final EmailPolicyService emailPolicyService;

    private final AccountConfirmationTokenServiceImpl accountConfirmationTokenService;
    private final AccountConfirmationAttemptServiceImpl accountConfirmationAttemptService;

    private final AccountDeactivationTokenServiceImpl accountDeactivationTokenService;
    private final AccountDeactivationAttemptServiceImpl accountDeactivationAttemptService;
    private final DeactivationPolicyService deactivationPolicyService;

    private final AccountReactivationTokenServiceImpl accountReactivationTokenService;

    private final UserService userService;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    private HttpServletRequest httpServletRequest;

    // =========================================================================
    // SIGNUP
    // =========================================================================

    @Override
    @Transactional
    public User signup(SelfSignupRequest request) {
        log.info("Processing self-signup for username: {}", request.email());

        User user = request.toEntity();

        SignupValidationResult validation = validateSignupData(user);
        if (!validation.isValid()) {
            log.warn("Signup validation failed for {}: {}", user.getUsername(), validation.errors());
            throw new SignupValidationException(
                    "Signup validation failed: " + String.join(", ", validation.errors()));
        }

        prepareUserForSelfSignup(user);
        userService.saveUser(user);
        log.info("User {} successfully created via self-signup", user.getUsername());

        AccountConfirmationToken token = accountConfirmationTokenService.createAccountConfirmationToken(user);
        publishSignupEvent(user, token);

        return user;
    }

    private SignupValidationResult validateSignupData(User user) {
        log.debug("Validating signup data for username: {}", user.getUsername());
        List<String> errors = new ArrayList<>();

        try {
            userService.checkIfUserExists(user);
        } catch (Exception e) {
            errors.add(e.getMessage());
        }

        EmailValidationResult emailResult = emailPolicyService.validateEmail(user.getEmail());
        if (!emailResult.isValid()) {
            errors.addAll(emailResult.errors());
        }

        PasswordValidationResult passwordResult = passwordPolicyService.validatePassword(user.getPassword());
        if (!passwordResult.isValid()) {
            errors.addAll(passwordResult.errors());
        }

        return new SignupValidationResult(errors.isEmpty(), errors);
    }

    private void prepareUserForSelfSignup(User user) {
        log.debug("Preparing user for self-signup mode");

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new SignupValidationException("Username is required for self-signup");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            user.setUserRoles(Set.of(UserRole.USER));
        }

        user.setEnabled(false);
        user.setEmailVerified(false);
        user.setEverActivated(false);
        user.setMustChangePassword(false);
        user.setProfileComplete(false);

        log.debug("User {} configured for self-signup", user.getUsername());
    }

    private void publishSignupEvent(User user, AccountConfirmationToken token) {
        Device device = deviceService.detectAndRegisterDevice(httpServletRequest, user);
        eventPublisher.publishEvent(new SelfSignupCompletedEvent(user, token.getPublicId(), device));
        log.info("SelfSignupCompletedEvent published for user: {}", user.getUsername());
    }

    // =========================================================================
    // ACTIVATION
    // =========================================================================

    @Override
    public User confirmNewUserAccount(String token) {
        AccountConfirmationToken confirmationToken =
                accountConfirmationTokenService.getSecureValidToken(token, TokenType.ACCOUNT_CONFIRMATION);
        User user = confirmationToken.getUser();

        if (user.isEnabled()) {
            throw new AccountAlreadyActivatedException(
                    "The user with email address " + user.getEmail() + " is already activated!");
        }

        userService.activateUser(user.getId());
        userService.setUserMailVerified(user);
        accountConfirmationTokenService.revokeAllUserTokens(user);
        accountConfirmationAttemptService.clearAttempts(user);

        Device device = deviceService.detectAndRegisterDevice(httpServletRequest, user);

        eventPublisher.publishEvent(new AccountConfirmationEvent(user, device));

        return user;
    }

    @Override
    public void resendActivation(String token) {
        AccountConfirmationToken oldToken = accountConfirmationTokenService.getSecureToken(token);
        User user = oldToken.getUser();

        if (user.isEnabled()) {
            throw new AccountAlreadyActivatedException(
                    "The user with email address " + user.getEmail() + " is already activated!");
        }

        AccountConfirmationToken newToken =
                accountConfirmationTokenService.createAccountConfirmationToken(user);

        Device device = deviceService.detectAndRegisterDevice(httpServletRequest, user);

        eventPublisher.publishEvent(new RequestAccountActivationEvent(user, newToken.getPublicId(), device));
    }


    @Override
    public void resendActivationByIdentifier(String identifier) {

        User user = userService.getUserByUsernameOrByEmail(identifier);

        if (user.isEnabled() || user.isEverActivated()) {
            log.debug("Resend activation ignored — account already active or was activated: {}", identifier);
            return; // Fail silently — pas d'info exploitable
        }

        AccountConfirmationToken newToken = accountConfirmationTokenService.createAccountConfirmationToken(user);

        Device device = deviceService.detectAndRegisterDevice(httpServletRequest, user);

        eventPublisher.publishEvent(new RequestAccountActivationEvent(user, newToken.getPublicId(), device));

    }



    // =========================================================================
    // DEACTIVATION
    // =========================================================================

    @Override
    public void requestDeactivation(User user, DeactivationRequest deactivationRequest) {
        if (!user.isEnabled()) {
            throw new AccountAlreadyDeactivatedException(
                    "The user with email address " + user.getEmail() + " is already deactivated!");
        }

        DeactivationValidationResult validationResult =
                deactivationPolicyService.validateDeactivationRequest(deactivationRequest, user);
        if (!validationResult.isValid()) {
            throw new InvalidDeactivationRequestException(
                    "Validation failed: " + String.join(", ", validationResult.errors()));
        }

        AccountDeactivationToken deactivationToken =
                accountDeactivationTokenService.createAccountDeactivationToken(
                        user,
                        deactivationRequest.deactivationReason(),
                        deactivationRequest.reasonDetails());

        Device device = deviceService.detectCurrentDevice(httpServletRequest);

        eventPublisher.publishEvent(new RequestAccountDeactivationEvent(
                user,
                deactivationToken.getPublicId(),
                deactivationRequest.deactivationReason(),
                deactivationRequest.reasonDetails(),
                device));
    }

    @Override
    public User deactivateAccount(String token) {
        AccountDeactivationToken deactivationToken =
                accountDeactivationTokenService.getSecureValidToken(token, TokenType.ACCOUNT_DEACTIVATION);
        User user = deactivationToken.getUser();

        if (!user.isEnabled()) {
            throw new AccountAlreadyDeactivatedException(
                    "The user with email address " + user.getEmail() + " is already deactivated!");
        }

        userService.deactivateUser(user.getId(),
                deactivationToken.getDeactivationReason(),
                deactivationToken.getReasonDetails());
        accountDeactivationTokenService.revokeAllUserTokens(user);
        accountDeactivationAttemptService.clearAttempts(user);

        Device device = deviceService.detectAndRegisterDevice(httpServletRequest, user);

        eventPublisher.publishEvent(new AccountDeactivationConfirmedEvent(
                user,
                deactivationToken.getDeactivationReason(),
                deactivationToken.getReasonDetails(),
                device));

        return user;
    }

    // =========================================================================
    // REACTIVATION
    // =========================================================================

    @Override
    public void requestReactivation(ReactivationRequest reactivationRequest) {
        User user = userService.getUserByUsernameOrByEmail(reactivationRequest.identifier());

        if (user.isEnabled()) {
            throw new AccountAlreadyActivatedException(
                    "The user with email address " + user.getEmail() + " is already activated!");
        }

        if (!determineReactivationEligibility(user)) {
            throw new ReactivationNotAllowedException(getReactivationDenialReason(user));
        }

        logReactivationAttempt(user, true);

        AccountReactivationToken reactivationToken =
                accountReactivationTokenService.createAccountReactivationToken(user);

        Device device = deviceService.detectAndRegisterDevice(httpServletRequest, user);

        eventPublisher.publishEvent(new RequestAccountReactivationEvent(
                user, reactivationToken.getPublicId(), device));
    }

    @Override
    public User reactivateAccount(String token) {
        AccountReactivationToken reactivationToken =
                accountReactivationTokenService.getSecureValidToken(token, TokenType.ACCOUNT_REACTIVATION);
        User user = reactivationToken.getUser();

        if (user.isEnabled()) {
            throw new AccountAlreadyActivatedException(
                    "The user with email address " + user.getEmail() + " is already activated!");
        }

        if (!determineReactivationEligibility(user)) {
            throw new ReactivationNotAllowedException(getReactivationDenialReason(user));
        }

        userService.reactivateUser(user);
        accountReactivationTokenService.revokeAllUserTokens(user);
        accountDeactivationAttemptService.clearAttempts(user);
        logReactivationAttempt(user, false);

        Device device = deviceService.detectAndRegisterDevice(httpServletRequest, user);

        eventPublisher.publishEvent(new AccountReactivationConfirmedEvent(user, device));

        return user;
    }

    // =========================================================================
    // PUBLIC UTILITY METHODS
    // =========================================================================

    public boolean canUserRequestReactivation(User user) {
        if (user == null || user.isEnabled()) return false;
        return determineReactivationEligibility(user);
    }

    public String getReactivationStatusMessage(User user) {
        if (user == null) return "User not found.";
        if (user.isEnabled()) return "Account is already active.";

        if (determineReactivationEligibility(user)) {
            return user.isAdminDeactivated()
                    ? String.format("Your account was deactivated by an administrator (%s). You can request reactivation.",
                    user.getAdminDeactivationReason().getDisplayName())
                    : "You can reactivate your account at any time.";
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

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private boolean determineReactivationEligibility(User user) {
        if (user.isAdminDeactivated()) {
            return user.getAdminDeactivationReason().allowsReactivation();
        } else if (user.isSelfDeactivated()) {
            return user.getDeactivationReason().allowsReactivation();
        } else {
            log.error("User {} is disabled but has no clear deactivation reason", user.getEmail());
            return false;
        }
    }

    private String getReactivationDenialReason(User user) {
        if (user.isAdminDeactivated()) {
            return String.format(
                    "Account reactivation is not allowed for admin deactivation category: %s. " +
                            "Contact an administrator for manual review.",
                    user.getAdminDeactivationReason().getDisplayName());
        } else if (user.isSelfDeactivated()) {
            return String.format(
                    "Account reactivation is not allowed for this deactivation reason: %s. %s",
                    user.getDeactivationReason().getDisplayName(),
                    user.getDeactivationReason() == DeactivationReason.GDPR_REQUEST
                            ? "This action is permanent and cannot be reversed."
                            : "Please contact support for assistance.");
        } else {
            return "Account deactivation reason unclear. Please contact support.";
        }
    }

    private void logReactivationAttempt(User user, boolean isRequest) {
        String action = isRequest ? "requested" : "completed";

        if (user.isAdminDeactivated()) {
            AdminDeactivationCategory category = user.getAdminDeactivationReason();
            String adminInfo = user.getAdminDeactivatedBy() != null
                    ? user.getAdminDeactivatedBy().getUsername() : "unknown";
            log.info("Reactivation {} for admin-deactivated user {} " +
                            "(category: {}, allows_reactivation: {}, deactivated_by: {})",
                    action, user.getEmail(), category.getDisplayName(),
                    category.allowsReactivation(), adminInfo);
        } else if (user.isSelfDeactivated()) {
            DeactivationReason reason = user.getDeactivationReason();
            log.info("Reactivation {} for self-deactivated user {} (reason: {}, allows_reactivation: {})",
                    action, user.getEmail(), reason.getDisplayName(), reason.allowsReactivation());
        } else {
            log.warn("Reactivation {} for user {} with unclear deactivation status",
                    action, user.getEmail());
        }
    }

//    /**
//     * Détecte et enregistre le device courant pour l'audit.
//     *
//     * On utilise {@link DeviceService#detectAndRegisterDevice} plutôt que
//     * {@code detectCurrentDevice} : si l'action vient d'un device inconnu
//     * (nouveau pays, nouveau navigateur...), il doit être enregistré pour
//     * avoir une trace complète — c'est précisément ce genre de contexte qui
//     * a de la valeur en sécurité.
//     *
//     * Retourne null silencieusement si la détection échoue : un device
//     * manquant ne doit jamais bloquer le flux métier.
//     */
//    private Device safeDetectDevice(User user) {
//        try {
//            return deviceService.detectCurrentDevice(httpServletRequest);
//        } catch (Exception e) {
//            log.warn("Could not detect device for user {} — activity log will have null device: {}",
//                    user.getUsername(), e.getMessage());
//            return null;
//        }
//    }
}