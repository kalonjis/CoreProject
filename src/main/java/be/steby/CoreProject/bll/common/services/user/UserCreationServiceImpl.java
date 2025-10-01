package be.steby.CoreProject.bll.common.services.user;

import be.steby.CoreProject.bll.common.events.user.AdminUserCreatedEvent;
import be.steby.CoreProject.bll.common.events.user.SelfSignupUserCreatedEvent;
import be.steby.CoreProject.bll.common.events.user.SystemUserCreatedEvent;
import be.steby.CoreProject.bll.common.exceptions.UserValidationException;
import be.steby.CoreProject.bll.common.models.user.UserCreationMode;
import be.steby.CoreProject.bll.common.models.user.UserCreationRequest;
import be.steby.CoreProject.bll.common.models.user.UserCreationResult;
import be.steby.CoreProject.bll.common.models.user.UserValidationResult;
import be.steby.CoreProject.bll.domains.account.services.tokens.confirmation.AccountConfirmationTokenServiceImpl;
import be.steby.CoreProject.bll.domains.emailaddress.models.EmailValidationResult;
import be.steby.CoreProject.bll.common.services.validation.email.EmailPolicyService;
import be.steby.CoreProject.bll.domains.password.models.PasswordValidationResult;
import be.steby.CoreProject.bll.common.services.validation.password.PasswordPolicyService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;
import be.steby.CoreProject.dl.enums.UserRole;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserCreationServiceImpl implements UserCreationService{

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final EmailPolicyService emailPolicyService;
    private final AccountConfirmationTokenServiceImpl accountConfirmationTokenService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UserCreationResult createUser(UserCreationRequest request) {
        log.info(" User creation - mode: {}, user: {}",
                request.mode(), request.user().getUsername());

        // 1. Full validation
        UserValidationResult validation = validateUserData(
                request.user(),
                request.password()
        );

        if(!validation.isValid()){
            log.warn("Validation failure for user {}: {}",
                    request.user().getUsername(), validation.errors());
            throw new UserValidationException(
                    "User validation failed" + String.join(", " , validation.errors())
            );
        }

        // 2. Handling Password according to mode
        String finalPassword = request.password();
        String temporaryPassword = null;

        if((request.mode() == UserCreationMode.ADMIN_CREATE || request.mode() == UserCreationMode.SYSTEM_CREATE) && finalPassword == null){
            temporaryPassword = passwordPolicyService.generateSecurePassword();
            finalPassword = temporaryPassword;
            log.debug("Temporary password generated for user {}", request.user().getUsername());
        }

        // 3. User preparation
        User user = request.user();
        user.setPassword(
                passwordEncoder.encode(finalPassword)
        );
        configureUserByMode(user, request.mode());

        // 4. Saving
        userService.saveUser(user);
        log.info("User {} successfully saved", user.getUsername());

        // 5. AccountConfirmation token creation according to mode
        String token = null;

        if(request.mode() != UserCreationMode.SYSTEM_CREATE){
            AccountConfirmationToken accountConfirmationToken = accountConfirmationTokenService.createAccountConfirmationToken(user);
            token = accountConfirmationToken.getPublicId();
        }
        // 6. Event publishing according to mode
        publishAppropriateEvent(user, request, temporaryPassword, token);

        return new UserCreationResult(user, temporaryPassword, token);
    }


    @Override
    public UserValidationResult validateUserData(User user, String password) {
        log.debug("User data validation for {}", user.getUsername());
        List<String> errors = new ArrayList<>();

        // 1. check if user already exists
        try{
            userService.checkIfUserExists(user);
        } catch (Exception e){
            errors.add(e.getMessage());
        }

        // 2. email validation
        EmailValidationResult emailResult = emailPolicyService.validateEmail(user.getEmail());
        if (!emailResult.isValid()){
            errors.addAll(emailResult.errors());
        }

        // 3. password validation
        if(password != null && !password.isBlank()){
           PasswordValidationResult passwordResult = passwordPolicyService.validatePassword(password);
            if (!passwordResult.isValid()) {
                errors.addAll(passwordResult.errors());
            }
        }

        boolean isValid = errors.isEmpty();
        log.debug("Validation utilisateur {} - Résultat: {}, Erreurs: {}",
                user.getUsername(), isValid, errors);

        return new UserValidationResult(isValid, errors);
    }


    /**
     * Config user according creation mode
     */
    private void configureUserByMode(User user, UserCreationMode mode){
        switch (mode) {
            case SELF_SIGNUP -> {
                user.setMustChangePassword(false);
                user.setEnabled(false);
                user.setEverActivated(false);
                log.debug("SELF_SIGNUP config applied");
            }
            case ADMIN_CREATE -> {
                user.setMustChangePassword(true);
                user.setEnabled(false);
                user.setEverActivated(false);
                log.debug("ADMIN_CREATE config applied");
            }
            case SYSTEM_CREATE -> {
                user.setEmailVerified(true);
                user.setMustChangePassword(true);
                user.setEnabled(true);
                user.setEverActivated(true);
                user.setActivatedAt(Instant.now());
                log.debug("SYSTEM_CREATE config applied");

            }
        }
    }


    /**
     * Validates the allocation of roles according to the authenticated user
     */
    private List<String> validateRoleAssignment(User user, UserRole authenticatedUserRole) {
        List<String> errors = new ArrayList<>();

        boolean isSuperAdmin = authenticatedUserRole == UserRole.SUPER_ADMIN;
        boolean userHasSuperAdmin = user.getUserRoles().contains(UserRole.SUPER_ADMIN);

        if (userHasSuperAdmin && !isSuperAdmin) {
            errors.add("Not enough authorities to create a user with SUPER_ADMIN role");
        }

        return errors;
    }

    private void publishAppropriateEvent(User user, UserCreationRequest request,
                                         String temporaryPassword, String token) {
        switch (request.mode()) {
            case SELF_SIGNUP -> {
                eventPublisher.publishEvent(
                        new SelfSignupUserCreatedEvent(user, token)
                );
                log.info("Événement SelfSignupUserCreatedEvent publié pour {}", user.getUsername());
            }
            case ADMIN_CREATE -> {
                eventPublisher.publishEvent(
                        new AdminUserCreatedEvent(user, token, temporaryPassword)
                );
                log.info("Événement AdminUserCreatedEvent publié pour {}", user.getUsername());
            }
            case SYSTEM_CREATE -> {
                eventPublisher.publishEvent(
                        new SystemUserCreatedEvent(user, temporaryPassword)
                );
                log.info("Événement SystemUserCreatedEvent publié pour {}", user.getUsername());
            }
        }
    }
}
