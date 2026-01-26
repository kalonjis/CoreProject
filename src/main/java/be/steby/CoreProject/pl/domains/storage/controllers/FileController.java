package be.steby.CoreProject.pl.domains.storage.controllers;

import be.steby.CoreProject.bll.domains.storage.models.FileUploadResult;
import be.steby.CoreProject.bll.domains.storage.services.FileMetadataService;
import be.steby.CoreProject.bll.domains.storage.services.FileStorageService;
import be.steby.CoreProject.dl.entities.StoredFile;
import be.steby.CoreProject.dl.enums.FileCategory;
import be.steby.CoreProject.pl.domains.storage.models.FileInfoResponse;
import be.steby.CoreProject.pl.domains.storage.models.FileOperationResponse;
import be.steby.CoreProject.pl.domains.storage.models.FileUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

/**
 * REST Controller for file storage operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>File upload</li>
 *   <li>File download/serving</li>
 *   <li>File metadata queries</li>
 *   <li>File deletion</li>
 * </ul>
 *
 * <p>Security: Most endpoints require authentication.
 * Public files can be served without authentication via dedicated endpoint.
 *
 * @see FileStorageService
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Files", description = "File storage and management operations")
public class FileController {

    private final FileStorageService fileStorageService;
    private final FileMetadataService fileMetadataService;

    // =========================================================================
    // Upload Endpoints
    // =========================================================================

    /**
     * Uploads a file.
     *
     * <p>The file is validated against the specified category rules.
     *
     * @param file        the file to upload
     * @param category    file category (AVATAR, DOCUMENT, PRODUCT_IMAGE)
     * @param description optional description/alt text
     * @param principal   authenticated user
     * @return upload result with file ID and access URL
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file", description = "Uploads a file with validation based on category")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "413", description = "File too large")
    })
    public ResponseEntity<FileUploadResponse> uploadFile(
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "File category", required = true)
            @RequestParam("category") FileCategory category,

            @Parameter(description = "Optional description")
            @RequestParam(value = "description", required = false) String description,

            Principal principal
    ) {
        log.info("File upload request: category={}, filename={}, size={}, user={}",
                category, file.getOriginalFilename(), file.getSize(), principal.getName());

        FileUploadResult result = fileStorageService.store(
                file,
                category,
                principal.getName(), // ownerPublicId - à adapter selon ton UserDetails
                "USER",
                description
        );

        return ResponseEntity.ok(FileUploadResponse.from(result));
    }

    // =========================================================================
    // Download/Serve Endpoints
    // =========================================================================

    /**
     * Downloads/serves a file.
     *
     * <p>Sets appropriate Content-Type and Content-Disposition headers.
     *
     * @param publicId the file's public ID
     * @return file content as Resource
     */
    @GetMapping("/{publicId}")
    @Operation(summary = "Download a file", description = "Retrieves file content by public ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File content"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "File public ID", required = true)
            @PathVariable String publicId
    ) {
        StoredFile metadata = fileMetadataService.getByPublicId(publicId);
        Resource resource = fileStorageService.loadAsResource(publicId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + metadata.getOriginalFilename() + "\"")
                .body(resource);
    }

    /**
     * Downloads a file as attachment (forces download).
     *
     * @param publicId the file's public ID
     * @return file content with attachment disposition
     */
    @GetMapping("/{publicId}/download")
    @Operation(summary = "Download file as attachment", description = "Forces browser to download the file")
    public ResponseEntity<Resource> downloadFileAsAttachment(
            @PathVariable String publicId
    ) {
        StoredFile metadata = fileMetadataService.getByPublicId(publicId);
        Resource resource = fileStorageService.loadAsResource(publicId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + metadata.getOriginalFilename() + "\"")
                .body(resource);
    }

    // =========================================================================
    // Metadata Endpoints
    // =========================================================================

    /**
     * Gets file metadata.
     *
     * @param publicId the file's public ID
     * @return file metadata
     */
    @GetMapping("/{publicId}/info")
    @Operation(summary = "Get file metadata", description = "Retrieves file information without content")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File metadata"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<FileInfoResponse> getFileInfo(
            @PathVariable String publicId
    ) {
        StoredFile file = fileMetadataService.getByPublicId(publicId);
        String accessUrl = fileStorageService.getAccessUrl(publicId);
        
        return ResponseEntity.ok(FileInfoResponse.from(file, accessUrl));
    }

    /**
     * Lists files for the authenticated user.
     *
     * @param category optional category filter
     * @param principal authenticated user
     * @return list of user's files
     */
    @GetMapping("/my")
    @Operation(summary = "List my files", description = "Lists all files owned by the authenticated user")
    public ResponseEntity<List<FileInfoResponse>> listMyFiles(
            @Parameter(description = "Filter by category")
            @RequestParam(value = "category", required = false) FileCategory category,

            Principal principal
    ) {
        List<StoredFile> files;
        
        if (category != null) {
            files = fileMetadataService.findByOwnerAndCategory(
                    principal.getName(), "USER", category);
        } else {
            files = fileMetadataService.findByOwner(principal.getName(), "USER");
        }

        List<FileInfoResponse> response = files.stream()
                .map(f -> FileInfoResponse.from(f, fileStorageService.getAccessUrl(f.getPublicId())))
                .toList();

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Delete Endpoints
    // =========================================================================

    /**
     * Deletes a file (soft delete).
     *
     * @param publicId  the file's public ID
     * @param principal authenticated user
     * @return operation result
     */
    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete a file", description = "Soft-deletes a file (can be restored)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File deleted"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete this file")
    })
    public ResponseEntity<FileOperationResponse> deleteFile(
            @PathVariable String publicId,
            Principal principal
    ) {
        // TODO: Add ownership verification
        log.info("File delete request: publicId={}, user={}", publicId, principal.getName());
        
        fileStorageService.delete(publicId);
        
        return ResponseEntity.ok(FileOperationResponse.deleted(publicId));
    }

    // =========================================================================
    // Storage Info Endpoints
    // =========================================================================

    /**
     * Gets storage usage for the authenticated user.
     *
     * @param principal authenticated user
     * @return storage statistics
     */
    @GetMapping("/storage/usage")
    @Operation(summary = "Get storage usage", description = "Returns storage statistics for the user")
    public ResponseEntity<StorageUsageResponse> getStorageUsage(Principal principal) {
        long bytesUsed = fileMetadataService.calculateStorageUsed(principal.getName(), "USER");
        long fileCount = fileMetadataService.countByOwner(principal.getName(), "USER");

        return ResponseEntity.ok(new StorageUsageResponse(bytesUsed, formatBytes(bytesUsed), fileCount));
    }

    // =========================================================================
    // Inner Response Classes
    // =========================================================================

    /**
     * Response for storage usage query.
     */
    public record StorageUsageResponse(
            long bytesUsed,
            String formattedSize,
            long fileCount
    ) {}

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}