package be.steby.CoreProject.bll.domains.userRegistration.services;

import be.steby.CoreProject.bll.common.models.user.UserCreationRequest;
import be.steby.CoreProject.bll.common.models.user.UserCreationResult;
import be.steby.CoreProject.bll.common.services.user.UserCreationService;
import be.steby.CoreProject.dl.entities.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserRegistrationServiceImpl implements UserRegistrationService{


    private final UserCreationService userCreationService;



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
//        return user;
        // REMPLACER tout le code existant par :
        UserCreationRequest req = UserCreationRequest.forSelfSignup(user, user.getPassword());
        UserCreationResult result = userCreationService.createUser(req);
        return result.user();
    }
}
