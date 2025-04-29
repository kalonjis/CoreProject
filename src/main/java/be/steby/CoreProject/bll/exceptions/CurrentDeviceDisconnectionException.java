package be.steby.CoreProject.bll.exceptions;

public class CurrentDeviceDisconnectionException extends CoreProjectException {
    public CurrentDeviceDisconnectionException(String message) {
        super(message, 400);
    }
}