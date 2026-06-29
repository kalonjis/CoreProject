package be.steby.CoreProject.bll.common.services.cookies;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Generic service for managing HTTP cookies.
 * Provides low-level cookie creation, configuration, and deletion.
 * This service is framework/infrastructure agnostic and can be reused across domains.
 */
@Service
@Slf4j
public class BaseCookieService {

    private static final String DEFAULT_PATH = "/";
    private static final String SAME_SITE    = "Lax";

    /**
     * Creates and adds a standard cookie to the HTTP response.
     * Cookie is NOT HttpOnly — suitable for client-side accessible cookies (e.g. XSRF).
     */
    public void setCookie(HttpServletResponse response,
                          String name,
                          String value,
                          int maxAgeSeconds,
                          String path,
                          boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path(path != null ? path : DEFAULT_PATH)
                .maxAge(maxAgeSeconds)
                .secure(secure)
                .httpOnly(false)
                .sameSite(SAME_SITE)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        log.debug("Standard cookie set: name={}, path={}, maxAge={}, secure={}",
                name, path, maxAgeSeconds, secure);
    }

    /**
     * Creates and adds a secure HttpOnly cookie to the HTTP response.
     * HttpOnly cookies cannot be accessed by JavaScript — suitable for JWT tokens.
     */
    public void setHttpOnlyCookie(HttpServletResponse response,
                                  String name,
                                  String value,
                                  int maxAgeSeconds,
                                  String path,
                                  boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path(path != null ? path : DEFAULT_PATH)
                .maxAge(maxAgeSeconds)
                .secure(secure)
                .httpOnly(true)
                .sameSite(SAME_SITE)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        log.debug("HttpOnly cookie set: name={}, path={}, maxAge={}, secure={}",
                name, path, maxAgeSeconds, secure);
    }

    /**
     * Deletes a cookie by setting its value to empty and maxAge to 0.
     * Must match the original cookie's path and security settings.
     */
    public void deleteCookie(HttpServletResponse response,
                             String name,
                             String path,
                             boolean secure,
                             boolean httpOnly) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .path(path != null ? path : DEFAULT_PATH)
                .maxAge(0)
                .secure(secure)
                .httpOnly(httpOnly)
                .sameSite(SAME_SITE)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        log.debug("Cookie deleted: name={}, path={}", name, path);
    }

    /**
     * Convenience method to delete an HttpOnly cookie.
     * Uses default path and secure=true.
     */
    public void deleteHttpOnlyCookie(HttpServletResponse response, String name) {
        deleteCookie(response, name, DEFAULT_PATH, true, true);
    }

    /**
     * Convenience method to delete a standard (non-HttpOnly) cookie.
     * Uses default path and secure=true.
     */
    public void deleteStandardCookie(HttpServletResponse response, String name) {
        deleteCookie(response, name, DEFAULT_PATH, true, false);
    }
}
