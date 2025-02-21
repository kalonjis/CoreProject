package be.steby.CoreProject.bll.exceptions;

public class EmailSendingException extends CoreProjectException {

    public EmailSendingException(String message) {
        super(message, 500);
    }

    public EmailSendingException(String message, int status) {
        super(message, status);
    }
}
