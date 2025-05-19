package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.bll.domains.password.exceptions.InvalidPasswordException;
import be.steby.CoreProject.bll.events.account.SignupEvent;
import be.steby.CoreProject.bll.exceptions.*;
import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.bll.services.RequestContextService;
import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.bll.services.security.AuthService;
import be.steby.CoreProject.dl.entities.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final RequestContextService requestContextService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;



    @Value("${url.front_server}")
    private String FRONT_URL;



    @Override
    public User signup(User user, HttpServletRequest request) {
        userService.checkIfUserExists(user);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.saveUser(user);

        RequestContext requestContext = requestContextService.captureRequestContext(request);
        eventPublisher.publishEvent( new SignupEvent( user, requestContext ));

        return user;
    }

    @Override
    public User login(String username, String password) {
        User user = (User) loadUserByUsername(username);
        if (!user.isEnabled()) {
            if (!user.isEverActivated()) {
                throw new AccountActivationException("Your account has never been activated. Please check your email and follow the activation instructions.", user.getUsername());
            } else {
                throw new UserEnabledStatusException("User account has been disabled by an administrator. Please contact support.", 403);
            }
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidPasswordException("Incorrect password");
        }
        return user;
    }

    @Override
    public void logout() {

    }

    @Override
    public User getAuthenticatedUser() {
        return userService.getAuthenticatedUser();
    }

    @Override
    public UserDetails loadUserByUsername(String username){
        return userService.getUserByUsername(username);
    }












}

