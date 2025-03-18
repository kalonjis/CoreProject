package be.steby.CoreProject.bll.exceptions;


public class NotEnoughAuthoritiesException extends CoreProjectException {


  public NotEnoughAuthoritiesException(String message) {
    super(message,403);
  }
}
