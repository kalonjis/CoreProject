package be.steby.CoreProject.dl.enums.crm;

/**
 * Telephony provider used to place or receive a call in the CRM.
 *
 * <p>Each value maps to a concrete {@code TelephonyAdapter} implementation
 * in the infrastructure layer. The adapter is selected at runtime based on
 * the tenant's {@link be.steby.CoreProject.dl.entities.crm.TelephonyConfig}.</p>
 *
 * <h3>Provider capabilities</h3>
 * <table border="1">
 *   <tr><th>Provider</th><th>In-app call</th><th>Auto-duration</th><th>Recording</th></tr>
 *   <tr><td>TEL_URI</td><td>No (OS handles)</td><td>No (manual confirm)</td><td>No</td></tr>
 *   <tr><td>TWILIO</td><td>Yes (WebRTC)</td><td>Yes (webhook)</td><td>Optional</td></tr>
 *   <tr><td>SIP</td><td>Yes (WebRTC)</td><td>Yes (SIP events)</td><td>Optional</td></tr>
 * </table>
 *
 * @see be.steby.CoreProject.dl.entities.crm.TelephonyConfig
 * @see be.steby.CoreProject.dl.entities.crm.CallSession
 */
public enum CallProvider {

    /**
     * Universal fallback — triggers the OS default dialer via a {@code tel:} URI.
     *
     * <p>Requires no credentials. Call duration and end time are confirmed
     * manually by the user through a post-call modal in the UI.</p>
     */
    TEL_URI,

    /**
     * Twilio Voice SDK — in-browser WebRTC calling via the Twilio platform.
     *
     * <p>Requires a Twilio Account SID, Auth Token, and a Twilio phone number.
     * Start time, duration, and status are reported automatically via webhooks.</p>
     */
    TWILIO,

    /**
     * Direct SIP/WebRTC — connects to the tenant's own SIP server using SIP.js.
     *
     * <p>Requires SIP server URI, username, and password.
     * Suitable for tenants with an existing VoIP subscription that exposes SIP credentials.</p>
     */
    SIP
}