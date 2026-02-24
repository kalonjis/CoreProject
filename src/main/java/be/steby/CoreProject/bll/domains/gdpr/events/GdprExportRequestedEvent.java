package be.steby.CoreProject.bll.domains.gdpr.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

// =============================================================================
// Event 1 — fired when user clicks "Request export"
// Listener sends: confirmation email with the link to confirm the request
// =============================================================================

/**
 * Published when a user initiates a GDPR export request.
 * The email listener sends a confirmation link to validate the request.
 *
 * @param user          the user who requested the export
 * @param confirmToken  the token embedded in the confirmation email link
 */
public record GdprExportRequestedEvent(
        User user,
        String confirmToken,
        Device device
) {}