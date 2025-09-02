package be.steby.CoreProject.bll.domains.auth.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when suspicious device activity is detected during authentication.
 * This includes scenarios such as:
 * <ul>
 *   <li>Login attempts from unusual geographical locations</li>
 *   <li>Suspicious behavioral patterns</li>
 *   <li>Device fingerprint anomalies</li>
 *   <li>Rapid login attempts from different locations</li>
 * </ul>
 *
 * <p>This exception results in login failure and typically triggers additional
 * security measures such as email confirmation or account verification.</p>
 *
 * <p>This is a temporary security restriction that should result in a 423 Locked response,
 * indicating that the account is temporarily locked pending security verification.
 * Once the user completes the required verification (e.g., email confirmation),
 * access will be restored.</p>
 *
 * @see DeviceSecurityException
 * @see BlacklistedDeviceException
 */
public class SuspiciousDeviceException extends DeviceSecurityException {

    /**
     * Constructs a new SuspiciousDeviceException with the specified detail message.
     * The HTTP status code is automatically set to 423 (Locked).
     *
     * @param message the detail message explaining why the device is considered suspicious
     *                and what action the user should take
     */
    public SuspiciousDeviceException(String message) {
        super(message, 423);
    }
}