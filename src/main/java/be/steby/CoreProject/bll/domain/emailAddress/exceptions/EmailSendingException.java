package be.steby.CoreProject.bll.domain.emailAddress.exceptions;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;

public class EmailSendingException extends CoreProjectException {

    public EmailSendingException(String message) {
        super(message, 500);
    }

    public EmailSendingException(String message, int status) {
        super(message, status);
    }
}
