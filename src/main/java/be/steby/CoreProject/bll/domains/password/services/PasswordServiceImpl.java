package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.common.services.validation.password.PasswordPolicyService;
import be.steby.CoreProject.bll.domains.password.events.PasswordChangedEvent;
import be.steby.CoreProject.bll.domains.password.events.RequestPasswordResetEvent;
import be.steby.CoreProject.bll.domains.password.events.RequestPasswordTokenEvent;
import be.steby.CoreProject.bll.domains.password.exceptions.InvalidPasswordException;
import be.steby.CoreProject.bll.domains.password.models.PasswordChangeRequest;
import be.steby.CoreProject.bll.domains.password.models.PasswordResetRequest;
import be.steby.CoreProject.bll.domains.password.models.PasswordValidationResult;
import be.steby.CoreProject.bll.exceptions.TokenValidityException;
import be.steby.CoreProject.bll.exceptions.UserAuthenticationStateException;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.password.services.tokens.PasswordResetTokenServiceImpl;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenServiceImpl passwordResetTokenService;
    private final PasswordPolicyService passwordPolicyService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${url.front_server}")
    private String FRONT_URL;



    @Transactional
    @Override
    public void resetPassword(PasswordResetRequest request, String token, HttpServletRequest httpRequest) {
        checkIsAnonymous();

        PasswordResetToken passwordResetToken = passwordResetTokenService.getValidToken(token, TokenType.PASSWORD_RESET);
        passwordResetTokenService.verifyTokenValidity(passwordResetToken);

        User user = passwordResetToken.getUser();

        PasswordValidationResult result = passwordPolicyService.validatePassword(request.password());
        if (!result.isValid()) {
            throw new InvalidPasswordException("Password doesn't meet security requirements: "
                    + String.join(", ", result.errors()));
        }


        savePassword(request.password(), user);

        passwordResetTokenService.revokeToken(passwordResetToken);
    }


    @Override
    public void changePassword(PasswordChangeRequest request, HttpServletRequest httpRequest) {
        User authenticatedUser = userService.getAuthenticatedUser();

        if(!passwordEncoder.matches(request.currentPassword(), authenticatedUser.getPassword())){
            throw new InvalidPasswordException("The current password is not correct", 400);
        }
        PasswordValidationResult result = passwordPolicyService.validatePassword(request.newPassword());
        if (!result.isValid()) {
            throw new InvalidPasswordException("Password doesn't meet security requirements: "
                    + String.join(", ", result.errors()));
        }


        savePassword(request.newPassword(), authenticatedUser);
    }



    @Override
    public void requestPasswordReset(String email, HttpServletRequest request) {
        checkIsAnonymous();

        User user = userService.getUserByEmail(email);
        PasswordResetToken passwordResetToken = passwordResetTokenService.createPasswordResetToken(user);

        eventPublisher.publishEvent(
                new RequestPasswordResetEvent(
                    user,passwordResetToken.getPublicId()
                )
        );
    }


    @Override
    public void requestPasswordToken(String token, HttpServletRequest httpRequest){
        checkIsAnonymous();

        PasswordResetToken passwordResetToken = passwordResetTokenService.getToken(token);
        if(passwordResetToken.isValid()) {
            String url = FRONT_URL + "/api/password/reset-password?token=" + token ;
            throw new TokenValidityException("This token, is still valid. Please follow this link: " + url);
        }
        User user = passwordResetToken.getUser();
        PasswordResetToken newToken = passwordResetTokenService.createPasswordResetToken(user);

        eventPublisher.publishEvent(
                new RequestPasswordTokenEvent(
                    user,
                    newToken.getPublicId()
                )
        );

        passwordResetTokenService.revokeToken(passwordResetToken);
    }


    private void savePassword(String password, User user){
        user.setPassword( passwordEncoder.encode(password) );
        if(user.isMustChangePassword()){
            user.setMustChangePassword(false);
        }
        userService.saveUser(user);

        eventPublisher.publishEvent(new PasswordChangedEvent(user));
    }

    private void checkIsAnonymous(){
        if(!userService.isAnonymous()) {
            String url = FRONT_URL + "/password/change-password";
            String message = "You are logged in. Please use the change password feature instead: ";
            throw new UserAuthenticationStateException(message + url, 403);
        }
    }

}
