package be.steby.CoreProject.bll.domains.account.exceptions;


import be.steby.CoreProject.bll.exceptions.CoreProjectException;

public class SelfActivationException extends CoreProjectException {


  public SelfActivationException(String message) {
    super(message,403);
  }
}
