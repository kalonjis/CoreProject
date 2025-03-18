package be.steby.CoreProject.bll.exceptions;


public class SelfActivationException extends CoreProjectException {


  public SelfActivationException(String message) {
    super(message,403);
  }
}
