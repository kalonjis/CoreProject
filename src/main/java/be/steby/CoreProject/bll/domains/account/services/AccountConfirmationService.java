package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.dl.entities.User;
import jakarta.servlet.http.HttpServletRequest;

public interface AccountConfirmationService {

    User confirmNewUserAccount(String token, HttpServletRequest request);

    void requestActivation(String token);

    void requestConfirmationLinkByUsername(String username);
}
