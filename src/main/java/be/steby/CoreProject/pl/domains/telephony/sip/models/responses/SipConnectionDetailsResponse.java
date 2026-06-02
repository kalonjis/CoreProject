package be.steby.CoreProject.pl.domains.telephony.sip.models.responses;

/**
 * Response containing everything SIP.js needs to register against Asterisk.
 *
 * <p>Returned only to the authenticated commercial for their own session.
 * Transmitted over HTTPS — the password is needed by SIP.js to authenticate
 * against the Asterisk WebSocket.</p>
 *
 * @param wsUrl       Asterisk WebSocket URL (e.g. {@code wss://pbx.example.com:8089/ws})
 * @param sipDomain   SIP domain / realm (e.g. {@code pbx.example.com})
 * @param sipUsername the commercial's SIP extension username
 * @param sipPassword the commercial's SIP extension password (decrypted, HTTPS only)
 * @param displayName SIP From display name
 */
public record SipConnectionDetailsResponse(
        String wsUrl,
        String sipDomain,
        String sipUsername,
        String sipPassword,
        String displayName
) {}
