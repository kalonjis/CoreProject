package be.steby.CoreProject.bll.domains.auth.exceptions;

public class OAuthOnlyAccountException extends AuthDomainException {
    public OAuthOnlyAccountException(String message) {
        super(message, 400);  // ou 403 selon ta préférence
    }
}