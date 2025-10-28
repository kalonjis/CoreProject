package be.steby.CoreProject.bll.domains.auth.exceptions;

public class InvalidOAuth2CredentialsException extends InvalidCredentialsException {

    public InvalidOAuth2CredentialsException(String message) {
        super(message);
    }
}
