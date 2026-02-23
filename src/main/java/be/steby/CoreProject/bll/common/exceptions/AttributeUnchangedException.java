package be.steby.CoreProject.bll.common.exceptions;


public class AttributeUnchangedException extends CoreProjectException {


  public AttributeUnchangedException(String message) {
    super(message,400);
  }
}
