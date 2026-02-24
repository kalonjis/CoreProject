package be.steby.CoreProject.bll.domains.gdpr.config;

import be.steby.CoreProject.bll.domains.gdpr.services.storage.GdprStorageService;
import be.steby.CoreProject.bll.domains.gdpr.services.storage.LocalGdprStorageServiceImpl;
//import be.steby.CoreProject.bll.domains.gdpr.services.storage.R2GdprStorageServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
//import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.S3Configuration;
//import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;

/**
 * Spring configuration for GDPR archive storage.
 *
 * <p>Selects the active {@link GdprStorageService} implementation
 * based on the {@code app.gdpr.storage.provider} property:
 *
 * <ul>
 *   <li>{@code local} — {@link LocalGdprStorageServiceImpl}: filesystem, zero external deps</li>
 *   <li>{@code r2}    — {@link R2GdprStorageServiceImpl}: Cloudflare R2 via S3-compatible API</li>
 * </ul>
 *
 * <p>Switching providers requires only a config change — no code modification.
 */
@Configuration
@Slf4j
public class GdprStorageConfig {

    // =========================================================================
    // LOCAL — active when app.gdpr.storage.provider=local
    // =========================================================================

    @Bean
    @ConditionalOnProperty(name = "app.gdpr.storage.provider", havingValue = "local")
    public GdprStorageService localGdprStorageService(
            @Value("${app.gdpr.storage.local.path}") String path) {
        log.info("GDPR Storage: LOCAL filesystem selected");
        return new LocalGdprStorageServiceImpl(path);
    }

    // =========================================================================
    // R2 — active when app.gdpr.storage.provider=r2
    // =========================================================================

//    @Bean
//    @ConditionalOnProperty(name = "app.gdpr.storage.provider", havingValue = "r2")
//    public S3Client r2S3Client(
//            @Value("${app.gdpr.storage.r2.endpoint}") String endpoint,
//            @Value("${app.gdpr.storage.r2.access-key}") String accessKey,
//            @Value("${app.gdpr.storage.r2.secret-key}") String secretKey) {
//        return S3Client.builder()
//            .endpointOverride(URI.create(endpoint))
//            .credentialsProvider(StaticCredentialsProvider.create(
//                AwsBasicCredentials.create(accessKey, secretKey)))
//            .region(Region.of("auto"))  // R2 requires "auto"
//            .serviceConfiguration(S3Configuration.builder()
//                .pathStyleAccessEnabled(true)  // Required for R2
//                .build())
//            .build();
//    }

//    @Bean
//    @ConditionalOnProperty(name = "app.gdpr.storage.provider", havingValue = "r2")
//    public S3Presigner r2S3Presigner(
//            @Value("${app.gdpr.storage.r2.endpoint}") String endpoint,
//            @Value("${app.gdpr.storage.r2.access-key}") String accessKey,
//            @Value("${app.gdpr.storage.r2.secret-key}") String secretKey) {
//        return S3Presigner.builder()
//            .endpointOverride(URI.create(endpoint))
//            .credentialsProvider(StaticCredentialsProvider.create(
//                AwsBasicCredentials.create(accessKey, secretKey)))
//            .region(Region.of("auto"))
//            .build();
//    }
//
//    @Bean
//    @ConditionalOnProperty(name = "app.gdpr.storage.provider", havingValue = "r2")
//    public GdprStorageService r2GdprStorageService(
//            S3Client r2S3Client,
//            S3Presigner r2S3Presigner,
//            @Value("${app.gdpr.storage.r2.bucket}") String bucket,
//            @Value("${app.gdpr.storage.r2.presigned-url-ttl-hours:72}") int ttlHours) {
//        log.info("GDPR Storage: Cloudflare R2 selected — bucket: {}", bucket);
//        return new R2GdprStorageServiceImpl(r2S3Client, r2S3Presigner, bucket, Duration.ofHours(ttlHours));
//    }
}