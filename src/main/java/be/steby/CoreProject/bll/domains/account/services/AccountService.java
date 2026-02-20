package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.bll.domains.account.models.DeactivationRequest;
import be.steby.CoreProject.bll.domains.account.models.ReactivationRequest;
import be.steby.CoreProject.bll.domains.account.models.SelfSignupRequest;
import be.steby.CoreProject.dl.entities.User;
import jakarta.servlet.http.HttpServletRequest;

public interface AccountService {

    User signup(SelfSignupRequest request);

    User confirmNewUserAccount(String token);

    void resendActivation(String token);

    void resendActivationByIdentifier(String identifier);

    void requestDeactivation(User user, DeactivationRequest deactivationRequest);

    User deactivateAccount(String token);

    User reactivateAccount(String token);

    void requestReactivation(ReactivationRequest reactivationRequest);
}
