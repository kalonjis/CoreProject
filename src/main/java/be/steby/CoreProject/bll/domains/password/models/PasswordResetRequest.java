package be.steby.CoreProject.bll.domains.password.models;

import be.steby.CoreProject.bll.domains.password.exceptions.PasswordValidationException;
import be.steby.CoreProject.pl.domains.password.models.ResetPasswordRequest;

public record PasswordResetRequest(
        String password
) {
    public PasswordResetRequest{
        if(password == null || password.isBlank()){
            throw new PasswordValidationException("Le mot de passe actuel ne peut pas être vide");
        }
    }

    public static PasswordResetRequest fromForm(ResetPasswordRequest form){
        return new PasswordResetRequest(form.password());
    }
}
