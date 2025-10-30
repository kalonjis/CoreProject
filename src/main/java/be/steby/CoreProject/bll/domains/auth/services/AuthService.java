package be.steby.CoreProject.bll.domains.auth.services;

import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidTwoFactorTokenException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.TwoFactorNotEnabledException;
import be.steby.CoreProject.bll.domains.auth.models.LoginInitiationResult;
import be.steby.CoreProject.bll.domains.auth.models.LoginTokens;
import be.steby.CoreProject.bll.domains.auth.models.TwoFactorMethodChosenResult;
import be.steby.CoreProject.bll.domains.auth.models.TwoFactorSessionInfo;
import be.steby.CoreProject.dl.entities.TwoFactorAuth;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.RefreshToken;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

/**
 * Service interface for managing user authentication and user-related operations.
 * This interface extends {@link UserDetailsService} to provide authentication-related functionality.
 */
public interface AuthService extends UserDetailsService {

    /**
     * Authenticates a user and detects device.
     *
     * @param username Username
     * @param password Password
     * @param request HTTP request for device detection
     * @return LoginTokens containing accessToken and refreshToken
     */
    LoginTokens login(String username, String password, HttpServletRequest request);


    /**
     * Phase 1: Initiate login - validate credentials and check 2FA requirements
     *
     * @param username User's username
     * @param password User's password
     * @param httpRequest HTTP request for device detection
     * @return LoginInitiationResult indicating next steps
     */
    LoginInitiationResult initiateLogin(String username, String password, HttpServletRequest httpRequest);

    /**
     * Phase 2: Verify 2FA code and complete login
     *
     * @param twoFactorToken JWT token from 2FA cookie
     * @param verificationCode 6-digit code from user
     * @param httpRequest HTTP request for context
     * @return LoginTokens for final authentication
     */
    LoginTokens verifyTwoFactorAndCompleteLogin(String twoFactorToken, String verificationCode, String backupCodes, HttpServletRequest httpRequest);

    /**
     * Resend 2FA verification code
     *
     * @param twoFactorToken JWT token from 2FA cookie
     * @param httpRequest HTTP request for context
     */
    void resendTwoFactorCode(String twoFactorToken, HttpServletRequest httpRequest);

    /**
     * Get information about current 2FA session
     *
     * @param twoFactorToken JWT token from 2FA cookie
     * @return TwoFactorSessionInfo with session details
     */
    TwoFactorSessionInfo getTwoFactorSessionInfo(String twoFactorToken);

    /**
     * Performs complete logout business logic including token revocation and events publishing.
     *
     * @param refreshTokenCookie The refresh token cookie value for token revocation
     * @param request The HTTP request for context capture and device detection
     */
    void logout(String refreshTokenCookie, HttpServletRequest request);

    /**
     * Retrieves the currently authenticated user.
     *
     * @return The authenticated user.
     */
    User getAuthenticatedUser();


    /**
     * Refreshes authentication tokens from a validated refresh token.
     *
     * @param validatedToken Validated refresh token
     * @return LoginTokens containing new access token and rotated refresh token
     */
    LoginTokens refreshAuthTokens(RefreshToken validatedToken);


    List<TwoFactorAuth> getAvailableTwoFactorMethods(User user);


    /**
     * Choose a two-factor authentication method and initiate verification process.
     *
     * Validates session token, generates verification code if needed (not for backup codes),
     * creates full 2FA token, and sets cookie. Used during authentication flow when
     * user selects their preferred 2FA method.
     *
     * @param twoFactorSessionToken lightweight JWT session token
     * @param chosenType the 2FA method chosen by user
     * @param httpRequest HTTP request for context
     * @throws InvalidTwoFactorTokenException if session token is invalid
     * @throws TwoFactorNotEnabledException if chosen method is not enabled for user
     */
    TwoFactorMethodChosenResult chooseTwoFactorMethod(String twoFactorSessionToken, TwoFactorType chosenType,
                                                      HttpServletRequest httpRequest);
}