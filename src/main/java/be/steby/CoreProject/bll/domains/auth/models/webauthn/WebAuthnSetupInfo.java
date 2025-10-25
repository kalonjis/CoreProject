package be.steby.CoreProject.bll.domains.auth.models.webauthn;

import java.util.List;
import java.util.Map;

/**
 * WebAuthn setup information returned during 2FA activation initiation.
 * 
 * Equivalent to TOTPSetupInfo but for WebAuthn authentication.
 * Contains all data needed for credential registration in the browser.
 * 
 * The challenge is temporary and used only for this registration session.
 * The credentialCreationOptions contain all WebAuthn parameters needed
 * by navigator.credentials.create() in the browser.
 * 
 * @param challenge Base64URL encoded cryptographic challenge
 * @param credentialCreationOptions Complete WebAuthn PublicKeyCredentialCreationOptions
 */
public record WebAuthnSetupInfo(
    String challenge,
    WebAuthnCredentialCreationOptions credentialCreationOptions
) {}