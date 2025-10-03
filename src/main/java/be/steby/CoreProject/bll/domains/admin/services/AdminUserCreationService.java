package be.steby.CoreProject.bll.domains.admin.services;

import be.steby.CoreProject.bll.common.services.validation.email.EmailPolicyService;
import be.steby.CoreProject.bll.common.services.validation.password.PasswordPolicyService;
import be.steby.CoreProject.bll.common.services.validation.textField.TextFieldValidationService;
import be.steby.CoreProject.bll.domains.emailaddress.models.EmailValidationResult;
import be.steby.CoreProject.bll.domains.admin.events.AdminUserCreatedEvent;
import be.steby.CoreProject.bll.domains.admin.exceptions.AdminOperationException;
import be.steby.CoreProject.bll.domains.admin.models.AdminUserCreationRequest;
import be.steby.CoreProject.bll.domains.admin.models.AdminUserCreationResult;
import be.steby.CoreProject.bll.domains.admin.models.AdminValidationResult;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.user.services.UsernameGeneratorService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service responsible for admin-initiated user creation.
 * Part of the Admin domain, handles the complete admin user creation flow.
 *
 * Flow:
 * 1. Validate admin request data
 * 2. Create user entity with admin-specific configuration
 * 3. Generate temporary password
 * 4. Save user to database
 * 5. Publish AdminUserCreatedEvent for email notification
 *
 * No confirmation token needed - user confirms by first login.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserCreationService {

    private final UserService userService;
    private final UsernameGeneratorService usernameGeneratorService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final EmailPolicyService emailPolicyService;
    private final TextFieldValidationService textFieldValidationService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates a new user account initiated by an administrator.
     *
     * Admin creation characteristics:
     * - Email is considered verified (admin verified identity)
     * - Account disabled until first login
     * - Temporary password generated automatically
     * - User must change password on first login
     * - Profile complete (firstname/lastname provided by admin)
     * - No confirmation token - user confirms by logging in
     *
     * @param request Admin user creation request with all required data
     * @return AdminUserCreationResult containing created user and temporary password
     * @throws AdminOperationException if validation fails
     */
    @Transactional
    public AdminUserCreationResult createUserByAdmin(AdminUserCreationRequest request) {
        log.info("Processing admin user creation for email: {}", request.email());

        // 1. Validate request data
        AdminValidationResult validation = validateAdminUserCreation(request);
        if (!validation.isValid()) {
            log.warn("Admin user creation validation failed: {}", validation.errors());
            throw new AdminOperationException(
                    "User creation validation failed: " + String.join(", ", validation.errors())
            );
        }

        // 2. Build user entity from request
        User user = buildUserFromRequest(request);

        // 3. Check if user already exists
        userService.checkIfUserExists(user);

        // 4. Generate temporary password
        String temporaryPassword = passwordPolicyService.generateSecurePassword();
        log.debug("Temporary password generated for user: {}", user.getUsername());

        // 5. Prepare user for admin creation
        prepareUserForAdminCreation(user, temporaryPassword);

        // 6. Save user
        userService.saveUser(user);
        log.info("User {} successfully created by admin", user.getUsername());

        // 7. Publish event for email sending (no token needed!)
        publishAdminCreationEvent(user, temporaryPassword);

        return new AdminUserCreationResult(user, temporaryPassword);
    }

    /**
     * Validates all admin user creation data.
     * Delegates to specialized validation services for each field type.
     */
    private AdminValidationResult validateAdminUserCreation(AdminUserCreationRequest request) {
        log.debug("Validating admin user creation data for email: {}", request.email());
        List<String> errors = new ArrayList<>();

        // Validate email using EmailPolicyService
        EmailValidationResult emailResult = emailPolicyService.validateEmail(request.email());
        if (!emailResult.isValid()) {
            errors.addAll(emailResult.errors());
        }

        // Validate firstname
        textFieldValidationService.validateFirstname(request.firstname(), errors);

        // Validate lastname
        textFieldValidationService.validateLastname(request.lastname(), errors);

        // Validate phone number if provided
        if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
            textFieldValidationService.validatePhoneNumber(request.phoneNumber(), errors);
        }

        // Validate roles are not empty
        if (request.userRoles() == null || request.userRoles().isEmpty()) {
            errors.add("At least one role must be assigned");
        }

        boolean isValid = errors.isEmpty();
        log.debug("Admin user creation validation result: valid={}, errors={}", isValid, errors);

        return new AdminValidationResult(isValid, errors);
    }

    /**
     * Builds User entity from AdminUserCreationRequest.
     */
    private User buildUserFromRequest(AdminUserCreationRequest request) {
        User user = new User();

        // Auto-generate professional username from full name
        String generatedUsername = usernameGeneratorService.generateFromFullName(
                request.firstname(),
                request.lastname()
        );
        user.setUsername(generatedUsername);
        log.debug("Auto-generated professional username: {}", generatedUsername);

        // Set user data from request
        user.setEmail(request.email());
        user.setFirstname(request.firstname());
        user.setLastname(request.lastname());
        user.setPhoneNumber(request.phoneNumber());

        // Set roles EXACTLY as requested (don't use UserRole.setRoles() for admin creation)
        // Admin explicitly chooses which roles to assign
        user.setUserRoles(new HashSet<>(request.userRoles()));

        return user;
    }

    /**
     * Prepares user entity for admin creation mode.
     * Sets all appropriate flags for admin-created users.
     *
     * Admin-created user characteristics:
     * - emailVerified = true (admin verified identity)
     * - enabled = false (until first login with temp password)
     * - everActivated = false (first activation pending)
     * - mustChangePassword = true (must change temp password)
     * - profileComplete = true (admin provided firstname/lastname)
     */
    private void prepareUserForAdminCreation(User user, String temporaryPassword) {
        log.debug("Preparing user for admin creation mode");

        // Encode temporary password
        user.setPassword(passwordEncoder.encode(temporaryPassword));

        // Configure account states for admin creation
        user.setEmailVerified(true);             // ✅ Admin verified identity
        user.setEnabled(false);                  // ❌ Until first login
        user.setEverActivated(false);            // First activation pending
        user.setMustChangePassword(true);        // ✅ Must change temporary password
        user.setProfileComplete(true);           // ✅ Admin provided firstname/lastname

        log.debug("User {} configured for admin creation: emailVerified=true, enabled=false, " +
                "mustChangePassword=true, profileComplete=true", user.getUsername());
    }

    /**
     * Publishes AdminUserCreatedEvent for async email notification.
     * No confirmation token needed - user confirms by logging in with temporary password.
     */
    private void publishAdminCreationEvent(User user, String temporaryPassword) {
        User admin = userService.getAuthenticatedUser();

        AdminUserCreatedEvent event = new AdminUserCreatedEvent(
                user,
                admin,
                temporaryPassword
        );

        eventPublisher.publishEvent(event);
        log.info("AdminUserCreatedEvent published for user: {} (created by: {})",
                user.getUsername(), admin.getUsername());
    }
}