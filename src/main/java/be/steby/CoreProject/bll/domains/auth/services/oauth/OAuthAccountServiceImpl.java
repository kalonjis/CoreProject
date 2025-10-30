package be.steby.CoreProject.bll.domains.auth.services.oauth;

import be.steby.CoreProject.bll.domains.auth.exceptions.AccountDisabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.PasswordChangeRequiredException;
import be.steby.CoreProject.bll.domains.user.services.UserService;
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

import java.util.Objects;
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
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Finds or creates a user for OAuth authentication with comprehensive email reconciliation.
     *
     * This method handles all OAuth login scenarios while respecting database constraints,
     * particularly the unique email constraint on the User table. It ensures proper account
     * reconciliation to prevent constraint violations and account fragmentation.
     *
     * Flow:
     * 1. Check if OAuthAccount exists by provider + providerUserId
     *    1.1. If exists and email unchanged → return existing user
     *    1.2. If exists but email changed → migrate OAuthAccount to correct user
     * 2. If no OAuthAccount exists → check for existing user by email
     *    2.1. If user exists → link new OAuthAccount to existing user
     *    2.2. If no user exists → create new user + OAuthAccount
     *
     * This approach prevents:
     * - Unique constraint violations on User.email
     * - Account fragmentation when users change emails
     * - Duplicate accounts across different OAuth providers
     * - Loss of user history during provider email updates
     *
     * @param provider OAuth provider (GITHUB, GOOGLE, MICROSOFT, etc.)
     * @param providerUserId Stable user ID from OAuth provider
     * @param email Current email from OAuth provider (may have changed)
     * @param username Username from OAuth provider
     * @param name Full name from OAuth provider (optional)
     * @return User entity (existing or newly created) with proper caching
     * @throws AccountDisabledException if target account is disabled
     */
    @Override
    @Transactional
    public User findOrCreateUserForOAuth(OAuthProvider provider, String providerUserId,
                                         String email, String username, String name) {
        log.info("OAuth login - provider: {}, providerUserId: {}, email: {}", provider, providerUserId, email);

        // 1. Check if OAuthAccount already exists for this provider + providerUserId
        Optional<OAuthAccount> existingOAuth = oauthAccountRepository
                .findByProviderAndProviderUserId(provider, providerUserId);

        if (existingOAuth.isPresent()) {
            OAuthAccount oauthAccount = existingOAuth.get();
            User currentUser = oauthAccount.getUser();
            String currentOAuthEmail = oauthAccount.getEmail();

            log.info("Found existing OAuth account for user: {} with stored email: {}",
                    currentUser.getUsername(), currentOAuthEmail);

            // 1.1. Compare stored email with current provider email
            if (Objects.equals(currentOAuthEmail, email)) {
                // Email unchanged - return existing user with cache update
                log.debug("Email unchanged for OAuth account, returning existing user");
                updateUserCache(currentUser);
                return currentUser;
            }

            // 1.2. Email changed at provider level - handle migration
            log.info("Email changed from '{}' to '{}' for OAuth provider {}",
                    currentOAuthEmail, email, provider);

            return handleEmailMigration(oauthAccount, currentUser, email, username, name);
        }

        // 2. No existing OAuthAccount - check for user by email reconciliation
        return handleNewOAuthAccount(provider, providerUserId, email, username, name);
    }

    /**
     * Handles email migration when OAuth provider email changes.
     * Either migrates OAuthAccount to existing user or creates new user.
     *
     * @param oauthAccount Existing OAuthAccount to migrate
     * @param currentUser Current user linked to OAuthAccount
     * @param newEmail New email from OAuth provider
     * @param username Username from OAuth provider
     * @param name Name from OAuth provider
     * @return Target user (existing or newly created)
     */
    private User handleEmailMigration(OAuthAccount oauthAccount, User currentUser,
                                      String newEmail, String username, String name) {
        // Check if user with new email already exists
        if (newEmail != null && !newEmail.isBlank() && !newEmail.contains("@oauth.local")) {
            Optional<User> userWithNewEmail = userRepository.findByEmailIgnoreCase(newEmail);

            if (userWithNewEmail.isPresent()) {
                User targetUser = userWithNewEmail.get();

                // Security check - cannot migrate to disabled account
                if (!targetUser.isEnabled()) {
                    log.warn("Cannot migrate OAuth - target account is disabled: {}", targetUser.getUsername());
                    throw new AccountDisabledException(
                            "Target account is disabled. Please contact support."
                    );
                }

                // Migrate OAuthAccount to existing user with new email
                log.info("Migrating OAuth account from user '{}' to user '{}' due to email change",
                        currentUser.getUsername(), targetUser.getUsername());

                oauthAccount.setUser(targetUser);
                oauthAccount.setEmail(newEmail);
                oauthAccountRepository.save(oauthAccount);

                updateUserCache(targetUser);
                return targetUser;
            }
        }

        // No user found with new email - create new user and migrate OAuthAccount
        log.info("No user found with new email '{}', creating new user for OAuth migration", newEmail);
        User newUser = createNewUserForOAuth(newEmail, username, name);

        // Migrate OAuthAccount to newly created user
        oauthAccount.setUser(newUser);
        oauthAccount.setEmail(newEmail);
        oauthAccountRepository.save(oauthAccount);

        updateUserCache(newUser);
        return newUser;
    }

    /**
     * Handles creation of new OAuthAccount with proper email reconciliation.
     *
     * @param provider OAuth provider
     * @param providerUserId Provider user ID
     * @param email Email from provider
     * @param username Username from provider
     * @param name Name from provider
     * @return User (existing or newly created)
     */
    private User handleNewOAuthAccount(OAuthProvider provider, String providerUserId,
                                       String email, String username, String name) {
        // Check if user with this email already exists (email reconciliation)
        if (email != null && !email.isBlank() && !email.contains("@oauth.local")) {
            Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email);

            if (existingUser.isPresent()) {
                User user = existingUser.get();

                // Security check - cannot link OAuth to disabled account
                if (!user.isEnabled()) {
                    log.warn("Cannot link OAuth - account is disabled: {}", user.getUsername());
                    throw new AccountDisabledException(
                            "Your account is disabled. Please contact support."
                    );
                }

                log.info("Found existing user by email, linking OAuth account - user: {}", user.getUsername());

                // Link new OAuthAccount to existing user
                createOAuthAccount(user, provider, providerUserId, email);
                updateUserCache(user);
                return user;
            }
        }

        // No existing user found - create new user and OAuthAccount
        log.info("Creating new user for OAuth login");
        User newUser = createNewUserForOAuth(email, username, name);
        createOAuthAccount(newUser, provider, providerUserId, email);
        updateUserCache(newUser);

        return newUser;
    }

    /**
     * Creates and saves a new OAuth account linked to a user.
     *
     * @param user User to link the OAuth account to
     * @param provider OAuth provider
     * @param providerUserId Provider's user ID
     * @param email Email from provider
     * @return Saved OAuthAccount
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
     * Creates a new user for OAuth authentication with proper caching.
     * OAuth users get a random password and cannot login via password.
     *
     * @param email Email from OAuth provider
     * @param username Username from OAuth provider
     * @param name Full name from OAuth provider
     * @return Newly created and cached user
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

        // Use UserService instead of repository to trigger UserPersistedEvent and caching
        User saved = userService.saveUser(user);
        log.info("New OAuth user created and cached - username: {}", saved.getUsername());
        return saved;
    }

    /**
     * Updates user cache by saving through UserService.
     * This triggers UserPersistedEvent for automatic cache management.
     *
     * @param user User to cache
     */
    private void updateUserCache(User user) {
        // Use UserService instead of direct repository to trigger caching events
        userService.saveUser(user);
        log.debug("User {} cached via UserService", user.getUsername());
    }
}