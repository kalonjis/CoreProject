package be.steby.CoreProject.bll.domains.auth.services.twofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.TwoFactorNotEnabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.TwoFactorServiceNotFoundException;
import be.steby.CoreProject.bll.domains.auth.models.CodeGenerationResult;
import be.steby.CoreProject.bll.domains.auth.services.twofactor.backupcodes.BackupCodesTwoFactorService;
import be.steby.CoreProject.bll.domains.auth.services.twofactor.emailtwofactor.EmailTwoFactorService;
import be.steby.CoreProject.bll.domains.auth.services.twofactor.smstwofactor.SmsTwoFactorService;
import be.steby.CoreProject.bll.domains.auth.services.twofactor.totptwofactor.TOTPTwoFactorService;
import be.steby.CoreProject.dal.repositories.TwoFactorAuthRepository;
import be.steby.CoreProject.dl.entities.TwoFactorAuth;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of TwoFactorFactory for managing 2FA operations.
 *
 * This factory coordinates between different 2FA services and provides a unified
 * interface for code generation and verification. It automatically determines
 * which service to use based on the user's primary 2FA configuration.
 *
 * Current supported 2FA methods:
 * - EMAIL: Delegated to EmailTwoFactorService
 * - TOTP: Delegated to TOTPTwoFactorService
 * - SMS: Delegated to SmsTwoFactorService
 *
 * Future 2FA methods (to be added):
 * - SMS: Will be delegated to SMSTwoFactorService
 * - WEBAUTHN: Will be delegated to WebAuthnTwoFactorService
 *
 * Architecture benefits:
 * - Extensible: Easy to add new 2FA methods by injecting new services
 * - Centralized: Single place for 2FA routing logic
 * - Testable: Each service can be mocked independently
 * - Maintainable: Changes to specific 2FA methods don't affect this factory
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorFactoryImpl implements TwoFactorFactory {

    private final TwoFactorAuthRepository twoFactorAuthRepository;
    private final EmailTwoFactorService emailTwoFactorService;
    private final TOTPTwoFactorService totpTwoFactorService;
    private final SmsTwoFactorService smsTwoFactorService;
    private final BackupCodesTwoFactorService backupCodesTwoFactorService;
    private final PasswordEncoder passwordEncoder; // For secure hashing

    // TODO: Inject other 2FA services as they are implemented
    // private final WebAuthnTwoFactorService webAuthnTwoFactorService;

    @Override
    public String generateCode(User user) {
        log.debug("Generating 2FA code for user: {}", user.getUsername());

        // Get user's primary 2FA method
        TwoFactorType primaryType = getPrimaryTwoFactorType(user)
                .orElseThrow(() -> {
                    log.warn("Attempted to generate code for user {} without enabled 2FA", user.getUsername());
                    return new TwoFactorNotEnabledException("No two-factor authentication method is enabled");
                });

        // Delegate to appropriate service based on type
        return switch (primaryType) {
            case EMAIL -> {
                log.debug("Generating EMAIL 2FA code for user: {}", user.getUsername());
                yield emailTwoFactorService.generateCode(user);
            }
            case TOTP -> {
                log.debug("Generating TOTP 2FA code for user: {}", user.getUsername());
                yield totpTwoFactorService.generateCode(user);
            }
            case SMS -> {
                log.debug("Generating SMS 2FA code for user: {}", user.getUsername());
                yield smsTwoFactorService.generateCode(user);
            }
            case WEBAUTHN -> {
                log.warn("WebAuthn 2FA not yet implemented for user: {}", user.getUsername());
                throw new TwoFactorServiceNotFoundException("WebAuthn two-factor authentication is not yet implemented");
            }
            case BACKUP_CODES -> {
                log.warn("Backup codes cannot generate new codes for user: {}", user.getUsername());
                throw new TwoFactorServiceNotFoundException("Backup codes cannot generate new verification codes");
            }
        };
    }

    @Override
    public CodeGenerationResult generateCodeForType(User user, TwoFactorType type) {
        log.debug("Generating 2FA code for user: {} with type: {}", user.getUsername(), type);

        // Verify this method is enabled for the user
        if (!isMethodEnabled(user, type)) {
            log.warn("Attempted to generate code for disabled 2FA method {} for user: {}", type, user.getUsername());
            throw new TwoFactorNotEnabledException("Two-factor method " + type + " is not enabled");
        }

        // Generate code using appropriate service
        String plainCode = switch (type) {
            case EMAIL -> {
                log.debug("Generating EMAIL 2FA code for user: {}", user.getUsername());
                yield emailTwoFactorService.generateCode(user);
            }
            case TOTP -> {
                log.debug("Generating TOTP 2FA code for user: {}", user.getUsername());
                yield totpTwoFactorService.generateCode(user);
            }
            case SMS -> {
                log.debug("Generating SMS 2FA code for specific type for user: {}", user.getUsername());
                yield smsTwoFactorService.generateCode(user);
            }
            case WEBAUTHN -> {
                log.warn("WebAuthn 2FA not yet implemented for user: {}", user.getUsername());
                throw new TwoFactorServiceNotFoundException("WebAuthn two-factor authentication is not yet implemented");
            }
            case BACKUP_CODES -> {
                log.warn("Backup codes cannot generate new codes for user: {}", user.getUsername());
                throw new TwoFactorServiceNotFoundException("Backup codes cannot generate new verification codes");
            }
        };

        // Hash the generated code
        String hashedCode = passwordEncoder.encode(plainCode);

        return new CodeGenerationResult(plainCode, hashedCode);
    }

    @Override
    public Optional<TwoFactorType> getPrimaryTwoFactorType(User user) {
        log.debug("Getting primary 2FA type for user: {}", user.getUsername());

        return twoFactorAuthRepository.findByUserAndIsPrimaryTrue(user)
                .map(TwoFactorAuth::getType);
    }

    @Override
    public boolean hasTwoFactorEnabled(User user) {
        log.debug("Checking if user {} has any 2FA method enabled", user.getUsername());

        return twoFactorAuthRepository.existsByUserAndEnabledTrue(user);
    }

    @Override
    public CodeGenerationResult generateCodeWithHash(User user) {
        log.debug("Generating 2FA code with hash for user: {}", user.getUsername());

        // Get user's primary 2FA method
        TwoFactorType primaryType = getPrimaryTwoFactorType(user)
                .orElseThrow(() -> {
                    log.warn("Attempted to generate code with hash for user {} without enabled 2FA", user.getUsername());
                    return new TwoFactorNotEnabledException("No two-factor authentication method is enabled");
                });

        String plainCode = generateCode(user);
        String hashedCode = passwordEncoder.encode(plainCode);

        return new CodeGenerationResult(plainCode, hashedCode);
    }

    @Override
    public boolean verifyTwoFactorCode(User user, String providedCode, String hashedCode, TwoFactorType chosenType) {
        log.debug("Verifying 2FA code for user: {} with type: {}", user.getUsername(), chosenType);

        // Route to appropriate service based on CHOSEN type (from JWT)
        return switch (chosenType) {
            case EMAIL -> passwordEncoder.matches(providedCode, hashedCode);
            case TOTP -> totpTwoFactorService.verifyCode(user, providedCode);
            case BACKUP_CODES -> backupCodesTwoFactorService.verifyCode(user, providedCode) ;
            case SMS -> passwordEncoder.matches(providedCode, hashedCode);
            case WEBAUTHN -> throw new TwoFactorServiceNotFoundException("WebAuthn not implemented");
        };
    }


    @Override
    public List<TwoFactorAuth> getEnabledTwoFactorMethods(User user) {
        log.debug("Getting enabled 2FA methods for user: {}", user.getUsername());

        List<TwoFactorAuth> enabledMethods = twoFactorAuthRepository.findByUserAndEnabledTrue(user);

        if (enabledMethods.isEmpty()) {
            log.warn("No enabled 2FA methods found for user: {}", user.getUsername());
            throw new TwoFactorNotEnabledException("No two-factor authentication methods are enabled");
        }

        log.debug("Found {} enabled 2FA methods for user: {}", enabledMethods.size(), user.getUsername());

        return enabledMethods;
    }


    @Override
    public boolean isMethodEnabled(User user, TwoFactorType type) {
        log.debug("Checking if 2FA method {} is enabled for user: {}", type, user.getUsername());
        return twoFactorAuthRepository.existsByUserAndTypeAndEnabledTrue(user, type);
    }
}