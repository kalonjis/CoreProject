package be.steby.CoreProject.bll.common.services.cookies;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Generic service for managing HTTP cookies.
 * Provides low-level cookie creation, configuration, and deletion.
 * This service is framework/infrastructure agnostic and can be reused across domains.
 */
@Service
@Slf4j
public class BaseCookieService {

    private static final String DEFAULT_PATH = "/";

    /**
     * Creates and adds a standard cookie to the HTTP response.
     * Cookie is NOT HttpOnly by default - suitable for client-side accessible cookies.
     *
     * @param response HTTP response to add cookie to
     * @param name Cookie name
     * @param value Cookie value
     * @param maxAgeSeconds Maximum age in seconds (time to live)
     * @param path Cookie path (defaults to "/")
     * @param secure Whether cookie should only be sent over HTTPS
     */
    public void setCookie(HttpServletResponse response,
                          String name,
                          String value,
                          int maxAgeSeconds,
                          String path,
                          boolean secure) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath(path != null ? path : DEFAULT_PATH);
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setSecure(secure);
        cookie.setHttpOnly(false); // Client-side accessible

        response.addCookie(cookie);
        log.debug("Standard cookie set: name={}, path={}, maxAge={}, secure={}",
                name, path, maxAgeSeconds, secure);
    }

    /**
     * Creates and adds a secure HttpOnly cookie to the HTTP response.
     * HttpOnly cookies cannot be accessed by JavaScript - suitable for security tokens.
     *
     * @param response HTTP response to add cookie to
     * @param name Cookie name
     * @param value Cookie value
     * @param maxAgeSeconds Maximum age in seconds (time to live)
     * @param path Cookie path (defaults to "/")
     * @param secure Whether cookie should only be sent over HTTPS
     */
    public void setHttpOnlyCookie(HttpServletResponse response,
                                  String name,
                                  String value,
                                  int maxAgeSeconds,
                                  String path,
                                  boolean secure) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath(path != null ? path : DEFAULT_PATH);
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setSecure(secure);
        cookie.setHttpOnly(true); // Protected from JavaScript access

        response.addCookie(cookie);
        log.debug("HttpOnly cookie set: name={}, path={}, maxAge={}, secure={}",
                name, path, maxAgeSeconds, secure);
    }

    /**
     * Deletes a cookie by setting its value to empty and maxAge to 0.
     * Must match the original cookie's path and security settings.
     *
     * @param response HTTP response to add deletion cookie to
     * @param name Cookie name to delete
     * @param path Cookie path (must match original cookie)
     * @param secure Security setting (must match original cookie)
     * @param httpOnly HttpOnly setting (must match original cookie)
     */
    public void deleteCookie(HttpServletResponse response,
                             String name,
                             String path,
                             boolean secure,
                             boolean httpOnly) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath(path != null ? path : DEFAULT_PATH);
        cookie.setMaxAge(0); // Immediate expiration
        cookie.setSecure(secure);
        cookie.setHttpOnly(httpOnly);

        response.addCookie(cookie);
        log.debug("Cookie deleted: name={}, path={}", name, path);
    }

    /**
     * Convenience method to delete an HttpOnly cookie.
     * Uses default path and secure=true.
     *
     * @param response HTTP response
     * @param name Cookie name to delete
     */
    public void deleteHttpOnlyCookie(HttpServletResponse response, String name) {
        deleteCookie(response, name, DEFAULT_PATH, true, true);
    }

    /**
     * Convenience method to delete a standard (non-HttpOnly) cookie.
     * Uses default path and secure=true.
     *
     * @param response HTTP response
     * @param name Cookie name to delete
     */
    public void deleteStandardCookie(HttpServletResponse response, String name) {
        deleteCookie(response, name, DEFAULT_PATH, true, false);
    }
}