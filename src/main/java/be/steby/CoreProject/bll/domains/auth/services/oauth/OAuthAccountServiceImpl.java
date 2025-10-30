package be.steby.CoreProject.bll.domains.auth.services.oauth;

import be.steby.CoreProject.bll.domains.auth.exceptions.AccountDisabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.PasswordChangeRequiredException;
import be.steby.CoreProject.dal.repositories.OAuthAccountRepository;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dl.entities.OAuthAccount;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.OAuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for managing OAuth accounts.
 * Handles linking OAuth providers to user accounts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthAccountServiceImpl implements OAuthAccountService {

    private final OAuthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User findOrCreateUserForOAuth(OAuthProvider provider, String providerUserId,
                                         String email, String username, String name) {
        log.info("OAuth login - provider: {}, email: {}", provider, email);

        // 1. Check if OAuthAccount already exists
        Optional<OAuthAccount> existingOAuth = oauthAccountRepository
                .findByProviderAndProviderUserId(provider, providerUserId);

        if (existingOAuth.isPresent()) {
            User user = existingOAuth.get().getUser();
            log.info("Found existing OAuth account for user: {}", user.getUsername());
            return user;
        }

        // 2. Check if user with email exists (automatic linking)
        if (email != null && !email.isBlank() && !email.contains("@oauth.local")) {
            Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email);

            if (existingUser.isPresent()) {
                User user = existingUser.get();

                // 🔒 SECURITY: Don't link OAuth if account is disabled
                if (!user.isEnabled()) {
                    log.warn("Cannot link OAuth - account is disabled: {}", user.getUsername());
                    throw new AccountDisabledException(
                            "Your account is disabled. Please contact support."
                    );
                }

                // 🔒 SECURITY: Don't link OAuth if password must be changed
                if (user.isMustChangePassword()) {
                    log.warn("Cannot link OAuth - user must change password first: {}", user.getUsername());
                    throw new PasswordChangeRequiredException(
                            "You must change your password before linking an OAuth account"
                    );
                }
                log.info("Found existing user by email, linking OAuth account - user: {}", user.getUsername());

                // Create and link OAuth account
                createOAuthAccount(user, provider, providerUserId, email);
                return user;
            }
        }

        // 3. Create new user + OAuth account
        log.info("Creating new user for OAuth login");
        User newUser = createNewUserForOAuth(email, username, name);
        createOAuthAccount(newUser, provider, providerUserId, email);

        return newUser;
    }

    /**
     * Creates and saves a new OAuth account linked to a user.
     */
    private OAuthAccount createOAuthAccount(User user, OAuthProvider provider,
                                            String providerUserId, String email) {
        OAuthAccount oauthAccount = new OAuthAccount();
        oauthAccount.setUser(user);
        oauthAccount.setProvider(provider);
        oauthAccount.setProviderUserId(providerUserId);
        oauthAccount.setEmail(email);

        OAuthAccount saved = oauthAccountRepository.save(oauthAccount);
        log.info("OAuth account created - userId: {}, provider: {}", user.getId(), provider);
        return saved;
    }

    /**
     * Creates a new user for OAuth authentication.
     * OAuth users get a random password (cannot login via password).
     */
    private User createNewUserForOAuth(String email, String username, String name) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email != null && !email.isBlank() ? email : username + "@oauth.local");
        user.setEmailVerified(email != null && !email.isBlank());

        // Parse name if provided
        if (name != null && !name.isBlank()) {
            String[] parts = name.trim().split(" ", 2);
            user.setFirstname(parts[0]);
            if (parts.length > 1) {
                user.setLastname(parts[1]);
            }
        }

        // OAuth users have random password (cannot login via password)
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setEnabled(true);
        user.setEverActivated(true);
        user.setMustChangePassword(false);

        User saved = userRepository.save(user);
        log.info("New OAuth user created - username: {}", saved.getUsername());
        return saved;
    }
}