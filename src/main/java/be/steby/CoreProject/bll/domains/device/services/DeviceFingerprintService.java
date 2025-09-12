package be.steby.CoreProject.bll.domains.device.services;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Service responsible for generating device fingerprints.
 * Uses a DRY approach with a single core method and wrapper methods for different sources.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceFingerprintService {

    private final UserAgentAnalyzer userAgentAnalyzer;

    /**
     * Core method for generating device fingerprint.
     * This is the single source of truth for fingerprint generation logic.
     */
    public String generateFingerprint(String userAgent, String acceptLanguage, String acceptEncoding,
                                      String screenResolution, String timezone, String platform, Long userId) {
        StringBuilder fingerprint = new StringBuilder();

        // 1. User Agent parsing
        if (userAgent != null) {
            UserAgent agent = userAgentAnalyzer.parse(userAgent);

            String osName = agent.getValue(UserAgent.OPERATING_SYSTEM_NAME);
            String osVersionMajor = agent.getValue(UserAgent.OPERATING_SYSTEM_VERSION_MAJOR);
            String browserName = agent.getValue(UserAgent.AGENT_NAME);
            String deviceClass = agent.getValue(UserAgent.DEVICE_CLASS);
            String deviceName = agent.getValue(UserAgent.DEVICE_NAME);
            String deviceBrand = agent.getValue(UserAgent.DEVICE_BRAND);

            appendIfValid(fingerprint, "OS", osName);

            // Don't use OS version if it's UNDEFINED
            if (osVersionMajor != null && !"UNDEFINED".equals(osVersionMajor)) {
                appendIfValid(fingerprint, "OSVer", osVersionMajor);
            }

            appendIfValid(fingerprint, "Browser", browserName);
            appendIfValid(fingerprint, "DeviceClass", deviceClass);
            appendIfValid(fingerprint, "DeviceName", deviceName);
            appendIfValid(fingerprint, "DeviceBrand", deviceBrand);
        } else {
            fingerprint.append("UserAgent:unknown");
        }

        // 2. HTTP Headers (stable for same device)
        String normalizedLanguage = normalizeAcceptLanguage(acceptLanguage);
        appendIfValid(fingerprint, "Lang", normalizedLanguage);
        appendIfValid(fingerprint, "Encoding", acceptEncoding);

        // 3. Additional headers (for advanced clients like Angular)
        if (screenResolution != null) {
            appendIfValid(fingerprint, "Screen", screenResolution);
        }
        if (timezone != null) {
            appendIfValid(fingerprint, "TZ", timezone);
        }
        if (platform != null) {
            appendIfValid(fingerprint, "Platform", platform);
        }

        // 4. User ID to avoid collisions between users
        fingerprint.append("_User:").append(userId);

        log.debug("Generated fingerprint components: {}", fingerprint.toString());

        // 5. Generate SHA-256 hash
        return generateSecureHash(fingerprint.toString());
    }

    /**
     * Wrapper method for HttpServletRequest (synchronous usage)
     */
    public String generateFingerprint(HttpServletRequest request, Long userId) {
        return generateFingerprint(
                request.getHeader("User-Agent"),
                request.getHeader("Accept-Language"),
                request.getHeader("Accept-Encoding"),
                request.getHeader("X-Screen-Resolution"),
                request.getHeader("X-Timezone"),
                request.getHeader("X-Platform"),
                userId
        );
    }

    /**
     * Appends a value to fingerprint only if it's valid
     */
    private void appendIfValid(StringBuilder fingerprint, String key, String value) {
        if (value != null && !value.isEmpty() &&
                !value.equals("??") && !value.equals("Unknown") && !value.equals("UNDEFINED")) {
            fingerprint.append("_").append(key).append(":").append(value);
        }
    }

    /**
     * Normalizes Accept-Language for stable comparison
     */
    private String normalizeAcceptLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isEmpty()) {
            return "unknown";
        }

        // Take only the primary language (before comma)
        if (acceptLanguage.contains(",")) {
            acceptLanguage = acceptLanguage.split(",")[0];
        }

        // Remove quality (q=0.9)
        if (acceptLanguage.contains(";")) {
            acceptLanguage = acceptLanguage.split(";")[0];
        }

        return acceptLanguage.trim().toLowerCase();
    }

    /**
     * Generates a secure SHA-256 hash
     */
    private String generateSecureHash(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback if SHA-256 is not available
            log.warn("SHA-256 not available, using fallback hash method", e);
            return String.valueOf(data.hashCode()) + "_" + System.currentTimeMillis();
        }
    }
}