package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.OAuthAccount;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing OAuth accounts.
 */
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {
    
    /**
     * Finds an OAuth account by provider and provider's user ID.
     * Used during OAuth login to check if account already exists.
     *
     * @param provider the OAuth provider
     * @param providerUserId the user ID from the provider
     * @return Optional containing the OAuth account if found
     */
    Optional<OAuthAccount> findByProviderAndProviderUserId(
        OAuthProvider provider, 
        String providerUserId
    );
    
    /**
     * Finds all OAuth accounts for a user.
     * Used to display linked accounts in user profile.
     *
     * @param user the user
     * @return list of OAuth accounts
     */
    List<OAuthAccount> findAllByUser(User user);
    
    /**
     * Finds an OAuth account by user and provider.
     * Used to check if user has already linked a specific provider.
     *
     * @param user the user
     * @param provider the OAuth provider
     * @return Optional containing the OAuth account if found
     */
    Optional<OAuthAccount> findByUserAndProvider(User user, OAuthProvider provider);
    
    /**
     * Checks if a user has any OAuth accounts.
     *
     * @param user the user
     * @return true if user has OAuth accounts, false otherwise
     */
    boolean existsByUser(User user);
    
    /**
     * Deletes all OAuth accounts for a user.
     * Used during user deletion.
     *
     * @param user the user
     */
    void deleteAllByUser(User user);
}