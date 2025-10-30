package be.steby.CoreProject.bll.domains.auth.services.oauth;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.OAuthProvider;

/**
 * Service interface for managing OAuth accounts.
 */
public interface OAuthAccountService {

    /**
     * Finds or creates user for OAuth login.
     *
     * Flow:
     * 1. Check if OAuthAccount exists -> return linked user
     * 2. Check if User with email exists -> link OAuth to existing user
     * 3. Create new User + link OAuth account
     *
     * @param provider OAuth provider
     * @param providerUserId provider's user ID
     * @param email user's email (may be null)
     * @param username username from provider
     * @param name full name from provider
     * @return User (existing or newly created)
     */
    User findOrCreateUserForOAuth(OAuthProvider provider, String providerUserId,
                                  String email, String username, String name);

}