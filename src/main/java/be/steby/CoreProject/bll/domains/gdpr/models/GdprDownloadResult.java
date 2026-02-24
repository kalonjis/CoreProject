package be.steby.CoreProject.bll.domains.gdpr.models;

/**
 * Result of a GDPR archive download operation.
 *
 * <p>The controller uses {@link #isRedirect()} to decide the response strategy:
 * <ul>
 *   <li>{@code true}  — R2 mode: issue HTTP 302 to {@link #redirectUrl()}</li>
 *   <li>{@code false} — local mode: stream {@link #fileBytes()} directly</li>
 * </ul>
 *
 * @param isRedirect   true if the controller should redirect, false if it should stream
 * @param redirectUrl  pre-signed URL to redirect to (R2 mode only, null otherwise)
 * @param fileBytes    raw ZIP content to stream (local mode only, null otherwise)
 * @param filename     suggested filename for the Content-Disposition header
 */
public record GdprDownloadResult(
        boolean isRedirect,
        String redirectUrl,
        byte[] fileBytes,
        String filename
) {

    /** Factory for local (stream) mode. */
    public static GdprDownloadResult stream(byte[] fileBytes, String filename) {
        return new GdprDownloadResult(false, null, fileBytes, filename);
    }

    /** Factory for remote (redirect) mode. */
    public static GdprDownloadResult redirect(String redirectUrl, String filename) {
        return new GdprDownloadResult(true, redirectUrl, null, filename);
    }
}