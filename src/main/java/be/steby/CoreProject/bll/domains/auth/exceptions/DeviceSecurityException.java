package be.steby.CoreProject.bll.domains.auth.exceptions;

/**
 * General exception for device security-related issues during authentication.
 * This serves as a base class for more specific device security exceptions.
 */
public class DeviceSecurityException extends AuthDomainException {

    public DeviceSecurityException(String message) {
        super(message);
    }
    public DeviceSecurityException(String message, int status) {
        super(message, status);
    }
}
