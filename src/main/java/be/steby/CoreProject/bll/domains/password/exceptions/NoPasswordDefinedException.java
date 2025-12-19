package be.steby.CoreProject.bll.domains.password.exceptions;

public class NoPasswordDefinedException extends PasswordDomainException {
    public NoPasswordDefinedException(String message) {
        super(message);
    }
}
