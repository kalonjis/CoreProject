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
import be.steby.CoreProject.bll.domains.account.services.tokens.deactivation.AccountDeactivationAttemptServiceImpl;
import be.steby.CoreProject.bll.domains.account.services.tokens.deactivation.AccountDeactivationTokenServiceImpl;
import be.steby.CoreProject.bll.domains.account.services.tokens.reactivation.AccountReactivationTokenServiceImpl;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.bll.domains.emailaddress.models.EmailValidationResult;
import be.steby.CoreProject.bll.domains.password.models.PasswordValidationResult;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.account.services.tokens.confirmation.AccountConfirmationAttemptServiceImpl;
import be.steby.CoreProject.bll.domains.account.services.tokens.confirmation.AccountConfirmationTokenServiceImpl;
import be.steby.CoreProject.bll.domains.user.services.UsernameGeneratorService;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final UsernameGeneratorService usernameGeneratorService;
    private final PasswordEncoder passwordEncoder;

    private final PasswordPolicyService passwordPolicyService;  // from bll.common.services.validation.password
    private final EmailPolicyService emailPolicyService;

    private final AccountConfirmationTokenServiceImpl accountConfirmationTokenService;
    private final AccountConfirmationAttemptServiceImpl accountConfirmationAttemptService;

    private final AccountDeactivationTokenServiceImpl accountDeactivationTokenService;
    private final AccountDeactivationAttemptServiceImpl accountDeactivationAttemptService;
    private final DeactivationPolicyService deactivationPolicyService;

    private final AccountReactivationTokenServiceImpl accountReactivationTokenService;

    private final UserService userService;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * Handles complete self-signup process.
     * Validates, creates user, generates token, and publishes event for email sending.
     */
    @Override
    @Transactional
    public User signup(SelfSignupRequest request, HttpServletRequest httpRequest) {
        log.info("Processing self-signup for username: {}", request.email());

        User user = request.toEntity();

        // 1. Validate signup data (delegates to existing domain policies)
        SignupValidationResult validation = validateSignupData(user);
        if (!validation.isValid()) {
            log.warn("Signup validation failed for {}: {}",
                    user.getUsername(), validation.errors());
            throw new SignupValidationException(
                    "Signup validation failed: " + String.join(", ", validation.errors())
            );
        }

        // 2. Prepare user for self-signup mode
        prepareUserForSelfSignup(user);

        // 3. Save user
        userService.saveUser(user);
        log.info("User {} successfully created via self-signup", user.getUsername());

        // 4. Generate activation token
        AccountConfirmationToken token =
                accountConfirmationTokenService.createAccountConfirmationToken(user);

        // 5. Publish event for email sending
        publishSignupEvent(user, token);

        return user;
    }

    /**
     * Validates all signup data.
     * ✅ DRY: Delegates to existing EmailPolicyService and PasswordPolicyService
     */
    private SignupValidationResult validateSignupData(User user) {
        log.debug("Validating signup data for username: {}", user.getUsername());
        List<String> errors = new ArrayList<>();

        // 1. Check if user already exists (username or email)
        try {
            userService.checkIfUserExists(user);
        } catch (Exception e) {
            errors.add(e.getMessage());
        }

        // 2. ✅ DELEGATION: Validate email using EmailPolicyService (password domain)
        EmailValidationResult emailResult = emailPolicyService.validateEmail(user.getEmail());
        if (!emailResult.isValid()) {
            errors.addAll(emailResult.errors());
        }

        // 3. ✅ DELEGATION: Validate password using PasswordPolicyService (password domain)
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            errors.add("Password is required");
        } else {
            PasswordValidationResult passwordResult =
                    passwordPolicyService.validatePassword(user.getPassword());
            if (!passwordResult.isValid()) {
                errors.addAll(passwordResult.errors());
            }
        }

        boolean isValid = errors.isEmpty();
        log.debug("Signup validation result for {}: valid={}, errors={}",
                user.getUsername(), isValid, errors);

        return new SignupValidationResult(isValid, errors);
    }

    /**
     * Prepares user entity for self-signup mode.
     * Auto-generates username if not provided (Discord-style).
     * Encodes password and sets appropriate account states.
     */
    private void prepareUserForSelfSignup(User user) {
        log.debug("Preparing user for self-signup mode");

        // Auto-generate username if not provided (NOT NULL constraint)
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            String generatedUsername = usernameGeneratorService.generateFromEmail(user.getEmail());
            user.setUsername(generatedUsername);
            log.info("Auto-generated username: {}", generatedUsername);
        } else {
            log.debug("Using provided username: {}", user.getUsername());
        }

        // Encode password
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // Set USER role if not already set
        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            user.setUserRoles(Set.of(UserRole.USER));
        }

        // Configure account states for self-signup
        user.setEnabled(false);                  // Disabled until email confirmation
        user.setEmailVerified(false);            // Email not yet verified
        user.setEverActivated(false);            // Never activated yet
        user.setMustChangePassword(false);       // User chose their password
        user.setProfileComplete(false);          // Profile not yet complete

        log.debug("User {} configured for self-signup: enabled=false, profileComplete=false",
                user.getUsername());
    }

    /**
     * Publishes signup event for async email sending.
     */
    private void publishSignupEvent(User user, AccountConfirmationToken token) {
        SelfSignupCompletedEvent event = new SelfSignupCompletedEvent(
                user,
                token.getPublicId()
        );
        eventPublisher.publishEvent(event);
        log.info("SelfSignupCompletedEvent published for user: {}", user.getUsername());
    }

    @Override
    public User confirmNewUserAccount(String token, HttpServletRequest request) {
        AccountConfirmationToken accountConfirmationToken = accountConfirmationTokenService.getSecureValidToken(token, TokenType.ACCOUNT_CONFIRMATION);
        User user = accountConfirmationToken.getUser();

        if (user.isEnabled()) {
            throw new AccountAlreadyActivatedException("The user with email address " + user.getEmail() + " is already activated!");
        }

        userService.activateUser(user.getId());
        userService.setUserMailVerified(user);
        accountConfirmationTokenService.revokeAllUserTokens(user);
        accountConfirmationAttemptService.clearAttempts(user);

        AccountConfirmationEvent event = new AccountConfirmationEvent(user);
        eventPublisher.publishEvent(event);

        return user;
    }

    /**
     * ✅ FIXED - Method signature matches interface: requestActivation
     */
    @Override
    public void resendActivation(String token, HttpServletRequest request) {

        AccountConfirmationToken oldToken = accountConfirmationTokenService.getSecureToken(token);
        User user = oldToken.getUser();

        if (user.isEnabled()) {
            throw new AccountAlreadyActivatedException("The user with email address " + user.getEmail() + " is already activated!");
        }

        AccountConfirmationToken accountConfirmationToken = accountConfirmationTokenService.createAccountConfirmationToken(user);

        RequestAccountActivationEvent event = new RequestAccountActivationEvent(user, accountConfirmationToken.getToken());
        eventPublisher.publishEvent(event);
    }

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

        RequestAccountDeactivationEvent event = new RequestAccountDeactivationEvent(
                user, accountDeactivationToken.getToken(),
                deactivationRequest.deactivationReason(),
                deactivationRequest.reasonDetails()
        );

        eventPublisher.publishEvent(event);
    }

    /**
     * ✅ INTERFACE METHOD - deactivateAccount
     */
    @Override
    public User deactivateAccount(String token, HttpServletRequest httpRequest) {
        AccountDeactivationToken accountDeactivationToken = accountDeactivationTokenService.getSecureValidToken(token, TokenType.ACCOUNT_DEACTIVATION);
        User user = accountDeactivationToken.getUser();

        if (!user.isEnabled()) {
            throw new AccountAlreadyDeactivatedException("The user with email address " + user.getEmail() + " is already deactivated!");
        }

        userService.deactivateUser(user.getId(), accountDeactivationToken.getDeactivationReason(), accountDeactivationToken.getReasonDetails());

        accountDeactivationTokenService.revokeAllUserTokens(user);

        accountDeactivationAttemptService.clearAttempts(user);

        AccountDeactivationConfirmedEvent event = new AccountDeactivationConfirmedEvent(
                user,
                accountDeactivationToken.getDeactivationReason(),
                accountDeactivationToken.getReasonDetails()
        );
        eventPublisher.publishEvent(event);

        return user;
    }


    @Override
    public void requestReactivation(ReactivationRequest reactivationRequest, HttpServletRequest httpRequest) {

        User user = userService.getUserByUsernameOrByEmail(reactivationRequest.identifier());
        if (user.isEnabled()) {
            throw new AccountAlreadyActivatedException("The user with email address " + user.getEmail() + " is already activated!");
        }

        boolean canReactivate = determineReactivationEligibility(user);

        if (!canReactivate) {
            String reason = getReactivationDenialReason(user);
            throw new ReactivationNotAllowedException(reason);
        }

        logReactivationAttempt(user, true);

        AccountReactivationToken accountReactivationToken = accountReactivationTokenService.createAccountReactivationToken(user);

        RequestAccountReactivationEvent event = new RequestAccountReactivationEvent(user, accountReactivationToken.getToken());
        eventPublisher.publishEvent(event);
    }

    /**
     * ✅ ULTRA-SIMPLIFIED - reactivateAccount method (interface signature)
     */
    @Override
    public User reactivateAccount(String token, HttpServletRequest httpRequest) {
        AccountReactivationToken accountReactivationToken = accountReactivationTokenService.getSecureValidToken(token, TokenType.ACCOUNT_REACTIVATION);
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

        AccountReactivationConfirmedEvent event = new AccountReactivationConfirmedEvent(user);
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