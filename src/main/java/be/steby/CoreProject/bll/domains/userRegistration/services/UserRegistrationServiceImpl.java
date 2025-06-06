package be.steby.CoreProject.bll.domains.userRegistration.services;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.models.user.UserCreationRequest;
import be.steby.CoreProject.bll.common.models.user.UserCreationResult;
import be.steby.CoreProject.bll.common.services.context.RequestContextService;
import be.steby.CoreProject.bll.common.services.user.UserCreationService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.events.account.SignupEvent;
import be.steby.CoreProject.dl.entities.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserRegistrationServiceImpl implements UserRegistrationService{


    private final UserCreationService userCreationService;
    private final RequestContextService requestContextService;



    /**
     * @param user
     * @param request
     * @return
     */
    @Override
    public User signup(User user, HttpServletRequest request) {
//        userService.checkIfUserExists(user);
//        user.setPassword(passwordEncoder.encode(user.getPassword()));
//        userService.saveUser(user);
//
        RequestContext requestContext = requestContextService.captureRequestContext(request);
//        eventPublisher.publishEvent( new SignupEvent( user, requestContext ));
//
//        return user;
        // REMPLACER tout le code existant par :
        UserCreationRequest req = UserCreationRequest.forSelfSignup(user, user.getPassword(), requestContext);
        UserCreationResult result = userCreationService.createUser(req);
        return result.user();
    }
}
