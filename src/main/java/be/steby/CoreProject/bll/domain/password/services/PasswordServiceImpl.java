package be.steby.CoreProject.bll.domain.password.services;

import be.steby.CoreProject.bll.domain.password.events.PasswordChangedEvent;
import be.steby.CoreProject.bll.domain.password.events.RequestPasswordResetEvent;
import be.steby.CoreProject.bll.domain.password.exceptions.InvalidPasswordException;
import be.steby.CoreProject.bll.domain.password.models.PasswordChangeRequest;
import be.steby.CoreProject.bll.domain.password.models.PasswordValidationResult;
import be.steby.CoreProject.bll.exceptions.TokenValidityException;
import be.steby.CoreProject.bll.exceptions.UserAuthenticationStateException;
import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.bll.services.RequestContextService;
import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.bll.services.security.impl.PasswordResetTokenServiceImpl;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import be.steby.CoreProject.pl.security.models.ChangePasswordForm;
import be.steby.CoreProject.pl.security.models.PasswordResetForm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenServiceImpl passwordResetTokenService;
    private final PasswordPolicyService passwordPolicyService;
    private final RequestContextService requestContextService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${url.front_server}")
    private String FRONT_URL;



    @Override
    public void resetPassword(PasswordResetForm form, String token, HttpServletRequest httpRequest) {
        checkIsAnonymous();

        PasswordResetToken passwordResetToken = passwordResetTokenService.getToken(token);
        passwordResetTokenService.verifyTokenValidity(passwordResetToken);
        passwordResetTokenService.revokeToken(passwordResetToken);

        User user = passwordResetToken.getUser();

        RequestContext requestContext = requestContextService.captureRequestContext(httpRequest);

        savePassword(form.password(), user, requestContext);
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

        RequestContext requestContext = requestContextService.captureRequestContext(httpRequest);

        savePassword(request.newPassword(), authenticatedUser,requestContext);
    }



    @Override
    public void requestPasswordReset(String email, HttpServletRequest request) {
        checkIsAnonymous();

        User user = userService.getUserByEmail(email);
        PasswordResetToken passwordResetToken = passwordResetTokenService.createPasswordResetToken(user);
        RequestContext requestContext = requestContextService.captureRequestContext(request);

        eventPublisher.publishEvent(new RequestPasswordResetEvent(
                user,
                passwordResetToken.getToken(),
                requestContext)
        );
    }


    @Override
    public void requestPasswordToken(String token){
        checkIsAnonymous();
        PasswordResetToken passwordResetToken = passwordResetTokenService.getToken(token);
        if(passwordResetToken.isValid()) {
            String url = FRONT_URL + "/api/password/reset-password?token=" + token ;
            throw new TokenValidityException("This token, is still valid. Please follow this link: " + url);
        }
        passwordResetTokenService.revokeToken(passwordResetToken);
        User user = passwordResetToken.getUser();
        PasswordResetToken newToken = passwordResetTokenService.createPasswordResetToken(user);
        //mailerService.sendPasswordResetRefresh(newToken.getToken(), user);
    }


    private void savePassword(String password, User user, RequestContext requestContext){
        user.setPassword( passwordEncoder.encode(password) );
        if(user.isMustChangePassword()){
            user.setMustChangePassword(false);
        }
        userService.saveUser(user);

        eventPublisher.publishEvent(new PasswordChangedEvent(user, requestContext));
    }

    private void checkIsAnonymous(){
        if(!userService.isAnonymous()) {
            String url = FRONT_URL + "/password/change-password";
            String message = "You are logged in. Please use the change password feature instead: ";
            throw new UserAuthenticationStateException(message + url, 403);
        }
    }

}
