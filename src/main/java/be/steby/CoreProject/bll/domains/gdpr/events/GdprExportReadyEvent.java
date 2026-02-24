package be.steby.CoreProject.bll.domains.gdpr.events;

import be.steby.CoreProject.dl.entities.User;

// =============================================================================
// Event 2 — fired when the ZIP archive is ready
// Listener sends: download-ready email with the direct download link
// =============================================================================

/**
 * Published when a GDPR export archive has been successfully generated.
 * The email listener sends the download link to the user.
 *
 * @param user          the user whose archive is ready
 * @param downloadToken the single-use token embedded in the download link
 * @param downloadUrl   the full frontend URL the user should click to download
 */
public record GdprExportReadyEvent(
        User user,
        String downloadToken,
        String downloadUrl
) {}