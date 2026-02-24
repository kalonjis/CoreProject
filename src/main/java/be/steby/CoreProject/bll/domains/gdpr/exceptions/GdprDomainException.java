package be.steby.CoreProject.bll.domains.gdpr.exceptions;

import be.steby.CoreProject.bll.common.exceptions.CoreProjectException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


public class GdprDomainException extends CoreProjectException {

    public GdprDomainException(String message) {
        super(message);
    }


    public GdprDomainException(String message, int status){
        super(message, status);
    }


    public GdprDomainException(String message, Throwable cause) {
        super(message, cause);
    }


    public GdprDomainException(String message, int status, Throwable cause) {
        super(message, status, cause);
    }
}