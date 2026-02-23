package be.steby.CoreProject.bll.domains.emailaddress.exceptions;

import be.steby.CoreProject.bll.common.exceptions.CoreProjectException;

public class EmailSendingException extends CoreProjectException {

    public EmailSendingException(String message) {
        super(message, 500);
    }

    public EmailSendingException(String message, int status) {
        super(message, status);
    }
}
