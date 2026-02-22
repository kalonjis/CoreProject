package be.steby.CoreProject.bll.domains.device.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for generating a stable device fingerprint for a given user.
 *
 * <p>The fingerprint is built from a combination of structural device characteristics
 * extracted from the HTTP request headers and the authenticated user's identifier.
 * It is designed to remain consistent across browser updates, OS minor updates,
 * window resizes, and network changes.</p>
 *
 * <p>Fingerprint components:</p>
 * <ul>
 *   <li>User identifier</li>
 *   <li>Device class (e.g. Mobile, Desktop, Tablet)</li>
 *   <li>Device brand (e.g. Apple, Samsung)</li>
 *   <li>Operating system class (e.g. Android, iOS, Windows)</li>
 *   <li>Browser family (e.g. Chrome, Safari, Firefox)</li>
 *   <li>Primary language derived from the Accept-Language header</li>
 * </ul>
 *
 * <p>Results are cached in-memory using Caffeine to avoid redundant User-Agent
 * parsing for repeated requests from the same user and device.</p>
 *
 * @see UserAgentAnalyzer
 */
@Service
@RequiredArgsConstructor
public class DeviceFingerprintService {

    private final UserAgentAnalyzer userAgentAnalyzer;

    /**
     * In-memory cache storing generated fingerprints.
     *
     * <ul>
     *   <li>Key   : {@code "userId|rawUserAgent"}</li>
     *   <li>Value : SHA-256 fingerprint hash</li>
     *   <li>Max   : 500 entries</li>
     *   <li>TTL   : 12 hours after last access</li>
     * </ul>
     */
    private final Cache<String, String> fingerprintCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(12, TimeUnit.HOURS)
            .build();

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Generates a device fingerprint from an incoming HTTP request and a user identifier.
     *
     * <p>Extracts the {@code User-Agent} and {@code Accept-Language} headers from the
     * request, parses the User-Agent, and delegates to
     * {@link #generateFingerprint(UserAgent, String, String, Long)}.</p>
     *
     * @param request  the incoming HTTP request containing device-related headers
     * @param userId   the authenticated user's unique identifier
     * @return a SHA-256 hex-encoded fingerprint string
     */
    public String generateFingerprint(HttpServletRequest request, Long userId) {
        String rawUserAgent   = StringUtils.defaultIfBlank(request.getHeader("User-Agent"), "");
        String acceptLanguage = request.getHeader("Accept-Language");
        UserAgent agent       = userAgentAnalyzer.parse(rawUserAgent);
        return generateFingerprint(agent, rawUserAgent, acceptLanguage, userId);
    }

    /**
     * Generates a device fingerprint from raw header string values and a user identifier.
     *
     * <p>Parses the {@code rawUserAgent} string internally and delegates to
     * {@link #generateFingerprint(UserAgent, String, String, Long)}.
     * Intended for unit testing without an {@link HttpServletRequest}.</p>
     *
     * @param rawUserAgent    the raw {@code User-Agent} header value
     * @param acceptLanguage  the raw {@code Accept-Language} header value
     * @param userId          the authenticated user's unique identifier
     * @return a SHA-256 hex-encoded fingerprint string
     */
    public String generateFingerprint(String rawUserAgent,
                                      String acceptLanguage,
                                      Long userId) {
        UserAgent agent = userAgentAnalyzer.parse(rawUserAgent);
        return generateFingerprint(agent, rawUserAgent, acceptLanguage, userId);
    }

    /**
     * Generates a device fingerprint from an already-parsed {@link UserAgent} and a user identifier.
     *
     * <p>Accepts a pre-parsed {@link UserAgent} to avoid redundant parsing when the caller
     * already holds a parsed instance. The raw User-Agent string is still required as the cache key.
     * Returns a cached result if one already exists for the {@code userId} and {@code rawUserAgent}
     * combination.</p>
     *
     * @param agent           the already-parsed {@link UserAgent} object
     * @param rawUserAgent    the raw {@code User-Agent} header value, used as the cache key
     * @param acceptLanguage  the raw {@code Accept-Language} header value
     * @param userId          the authenticated user's unique identifier
     * @return a SHA-256 hex-encoded fingerprint string
     */
    public String generateFingerprint(UserAgent agent,
                                      String rawUserAgent,
                                      String acceptLanguage,
                                      Long userId) {
        String cacheKey = userId + "|" + rawUserAgent;
        return fingerprintCache.get(cacheKey, key ->
                buildFingerprint(agent, acceptLanguage, userId));
    }

    // ─── Fingerprint Building ─────────────────────────────────────────────────

    /**
     * Builds the fingerprint by assembling the parsed agent components into a
     * pipe-delimited string and hashing it with SHA-256.
     *
     * @param agent           the already-parsed {@link UserAgent} object
     * @param acceptLanguage  the raw {@code Accept-Language} header value
     * @param userId          the authenticated user's unique identifier
     * @return a SHA-256 hex-encoded fingerprint string
     */
    private String buildFingerprint(UserAgent agent,
                                    String acceptLanguage,
                                    Long userId) {
        String deviceClass   = getStableValue(agent, UserAgent.DEVICE_CLASS);
        String deviceBrand   = getStableValue(agent, UserAgent.DEVICE_BRAND);
        String osClass       = getStableValue(agent, UserAgent.OPERATING_SYSTEM_CLASS);
        String browserFamily = getStableValue(agent, UserAgent.AGENT_NAME);
        String language      = normalizeAcceptLanguage(acceptLanguage);

        String data = String.join("|",
                userId.toString(), deviceClass, deviceBrand, osClass, browserFamily, language);

        return generateSecureHash(data);
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    /**
     * Extracts a field value from a parsed {@link UserAgent}.
     *
     * <p>Returns {@code "UNKNOWN"} if the value is blank, {@code "??"}, {@code "Unknown"},
     * or {@code "UNDEFINED"}, ensuring unresolved fields do not introduce noise
     * into the fingerprint.</p>
     *
     * @param agent  the parsed UserAgent object
     * @param field  the Yauaa field name to extract (e.g. {@code UserAgent.DEVICE_CLASS})
     * @return the field value, or {@code "UNKNOWN"} if unresolvable
     */
    private String getStableValue(UserAgent agent, String field) {
        String value = agent.getValue(field);
        if (StringUtils.isBlank(value)
                || value.equals("??")
                || value.equalsIgnoreCase("Unknown")
                || value.equalsIgnoreCase("UNDEFINED")) {
            return "UNKNOWN";
        }
        return value;
    }

    /**
     * Normalizes an {@code Accept-Language} header value to its primary ISO 639-1 language code.
     *
     * <p>For example, {@code "fr-FR,fr;q=0.9,en-US;q=0.8"} is normalized to {@code "fr"}.
     * Returns {@code "UNKNOWN"} if the header is blank or contains no valid two-letter code.</p>
     *
     * @param acceptLanguage  the raw {@code Accept-Language} header value
     * @return the primary two-letter language code, or {@code "UNKNOWN"}
     */
    private String normalizeAcceptLanguage(String acceptLanguage) {
        if (StringUtils.isBlank(acceptLanguage)) return "UNKNOWN";

        return Arrays.stream(acceptLanguage.split(","))
                .map(String::trim)
                .map(lang -> lang.split(";")[0].trim())
                .map(lang -> lang.split("-")[0].trim())
                .filter(lang -> lang.length() == 2)
                .findFirst()
                .orElse("UNKNOWN");
    }

    /**
     * Computes a SHA-256 hash of the given string and returns it as a lowercase hex string.
     *
     * @param data  the input string to hash
     * @return a 64-character lowercase hex-encoded SHA-256 hash
     * @throws IllegalStateException if SHA-256 is not available on the current JVM
     */
    private String generateSecureHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable on this JVM", e);
        }
    }
}