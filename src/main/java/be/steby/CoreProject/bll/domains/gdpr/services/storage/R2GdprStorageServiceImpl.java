//package be.steby.CoreProject.bll.domains.gdpr.services.storage;
//
//import lombok.extern.slf4j.Slf4j;
//import software.amazon.awssdk.core.sync.RequestBody;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.*;
//import software.amazon.awssdk.services.s3.presigner.S3Presigner;
//import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
//
//import java.time.Duration;
//
///**
// * Cloudflare R2 implementation of {@link GdprStorageService}.
// * Activated when: {@code app.gdpr.storage.provider=r2}
// *
// * <p>Archives are stored in a private R2 bucket.
// * Downloads are served via a pre-signed URL valid for a configurable TTL (default: 72h).
// *
// * <p>R2 is S3-compatible — the AWS SDK is used with a custom endpoint override.
// *
// * <p>Required Maven dependency:
// * <pre>
// * {@code
// * <dependency>
// *     <groupId>software.amazon.awssdk</groupId>
// *     <artifactId>s3</artifactId>
// * </dependency>
// * }
// * </pre>
// */
//@Slf4j
//public class R2GdprStorageServiceImpl implements GdprStorageService {
//
//    private static final String KEY_PREFIX = "gdpr-exports/";
//
//    private final S3Client s3;
//    private final S3Presigner presigner;
//    private final String bucket;
//    private final Duration presignedUrlTtl;
//
//    public R2GdprStorageServiceImpl(S3Client s3, S3Presigner presigner,
//                                    String bucket, Duration presignedUrlTtl) {
//        this.s3 = s3;
//        this.presigner = presigner;
//        this.bucket = bucket;
//        this.presignedUrlTtl = presignedUrlTtl;
//        log.info("GDPR R2 storage initialized — bucket: {}, presign TTL: {}", bucket, presignedUrlTtl);
//    }
//
//    @Override
//    public void save(String fileId, byte[] data) {
//        s3.putObject(
//            PutObjectRequest.builder()
//                .bucket(bucket)
//                .key(key(fileId))
//                .contentType("application/zip")
//                .contentLength((long) data.length)
//                .build(),
//            RequestBody.fromBytes(data)
//        );
//        log.debug("GDPR archive uploaded to R2: {}", key(fileId));
//    }
//
//    @Override
//    public String getDownloadUrl(String fileId) {
//        var presignRequest = GetObjectPresignRequest.builder()
//            .signatureDuration(presignedUrlTtl)
//            .getObjectRequest(r -> r.bucket(bucket).key(key(fileId)))
//            .build();
//
//        String url = presigner.presignGetObject(presignRequest).url().toString();
//        log.debug("Pre-signed URL generated for: {} (TTL: {})", fileId, presignedUrlTtl);
//        return url;
//    }
//
//    @Override
//    public byte[] load(String fileId) {
//        // In R2 mode the controller redirects to the pre-signed URL,
//        // so load() is normally never called. Implemented to satisfy the interface contract.
//        try {
//            return s3.getObjectAsBytes(
//                GetObjectRequest.builder().bucket(bucket).key(key(fileId)).build()
//            ).asByteArray();
//        } catch (NoSuchKeyException e) {
//            throw new GdprStorageException("GDPR archive not found in R2: " + fileId, e);
//        }
//    }
//
//    @Override
//    public void delete(String fileId) {
//        try {
//            s3.deleteObject(
//                DeleteObjectRequest.builder().bucket(bucket).key(key(fileId)).build()
//            );
//            log.debug("GDPR archive deleted from R2: {}", key(fileId));
//        } catch (Exception e) {
//            log.warn("Failed to delete GDPR archive from R2: {}", fileId, e);
//        }
//    }
//
//    @Override
//    public boolean isRemote() {
//        return true;
//    }
//
//    // =========================================================================
//    // Private
//    // =========================================================================
//
//    private String key(String fileId) {
//        return KEY_PREFIX + fileId + ".zip";
//    }
//}