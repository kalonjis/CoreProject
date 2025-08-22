package be.steby.CoreProject.bll.domains.emailAddress.services;

import be.steby.CoreProject.pl.models.emailAddress.ChangeEmailForm;
import jakarta.servlet.http.HttpServletRequest;

public interface EmailAddressService {

    void changeEmailRequest(ChangeEmailForm form, HttpServletRequest request);

    void cancelEmailChange(String token, HttpServletRequest request);

    void changeEmailVerification(String token, HttpServletRequest request);

    void confirmEmail(String token, HttpServletRequest request);

}