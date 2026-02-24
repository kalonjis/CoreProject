package be.steby.CoreProject.bll.domains.gdpr.services.storage;

/**
 * Abstraction layer for GDPR archive storage.
 *
 * <p>Two implementations are available:
 * <ul>
 *   <li>{@link LocalGdprStorageServiceImpl} — local filesystem (dev / bare-metal)</li>
 *   <li>{@link R2GdprStorageServiceImpl} — Cloudflare R2 via S3-compatible API (prod)</li>
 * </ul>
 *
 * <p>The active implementation is selected via: {@code app.gdpr.storage.provider}
 */
public interface GdprStorageService {

    /**
     * Stores a ZIP archive.
     *
     * @param fileId unique identifier for the archive (= downloadToken from GdprExportRequest)
     * @param data   binary content of the ZIP archive
     */
    void save(String fileId, byte[] data);

    /**
     * Returns the URL or identifier used to serve the archive.
     *
     * <ul>
     *   <li>Local: returns the fileId — the controller serves the file directly</li>
     *   <li>R2: returns a pre-signed URL — the controller redirects with HTTP 302</li>
     * </ul>
     *
     * @param fileId unique identifier for the archive
     * @return download URL or internal identifier
     */
    String getDownloadUrl(String fileId);

    /**
     * Loads the binary content of an archive.
     * Called by the controller in local mode only.
     *
     * @param fileId unique identifier for the archive
     * @return binary content of the archive
     * @throws GdprStorageException if the file does not exist
     */
    byte[] load(String fileId);

    /**
     * Deletes an archive from storage.
     * Called on expiration or after download.
     *
     * @param fileId unique identifier for the archive
     */
    void delete(String fileId);

    /**
     * Indicates whether this implementation uses remote storage (R2, S3, etc.).
     * The controller uses this flag to decide between serving the file directly
     * or issuing an HTTP 302 redirect to a pre-signed URL.
     *
     * @return true if storage is remote, false if local
     */
    boolean isRemote();
}