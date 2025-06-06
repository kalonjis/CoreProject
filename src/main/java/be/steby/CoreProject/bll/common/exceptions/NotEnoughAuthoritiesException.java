package be.steby.CoreProject.bll.common.exceptions;


import be.steby.CoreProject.bll.exceptions.CoreProjectException;

public class NotEnoughAuthoritiesException extends CoreProjectException {


  public NotEnoughAuthoritiesException(String message) {
    super(message,403);
  }
}
