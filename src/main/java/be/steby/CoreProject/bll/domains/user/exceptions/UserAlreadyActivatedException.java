package be.steby.CoreProject.bll.domains.user.exceptions;

public class UserAlreadyActivatedException extends UserDomainException {
    public UserAlreadyActivatedException(String message) {
        super(message, 409   );
    }
}