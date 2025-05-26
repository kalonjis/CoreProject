package be.steby.CoreProject.bll.domains.userRegistration.services;

import be.steby.CoreProject.dl.entities.User;
import jakarta.servlet.http.HttpServletRequest;

public interface UserRegistrationService {

    User signup(User user, HttpServletRequest request);

}
