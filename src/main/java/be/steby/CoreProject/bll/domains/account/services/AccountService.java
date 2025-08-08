package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.bll.domains.account.models.DeactivationRequest;
import be.steby.CoreProject.dl.entities.User;
import jakarta.servlet.http.HttpServletRequest;

public interface AccountService {
    User confirmNewUserAccount(String token, HttpServletRequest request);

    void requestActivation(String token, HttpServletRequest request);

    void requestDeactivation(User user, DeactivationRequest deactivationRequest, HttpServletRequest request);

    User deactivateAccount(String token, HttpServletRequest httpRequest);

    void requestReactivation(User user,HttpServletRequest httpRequest);
}
