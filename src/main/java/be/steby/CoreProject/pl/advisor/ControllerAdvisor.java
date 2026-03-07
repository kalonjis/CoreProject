package be.steby.CoreProject.pl.advisor;

import be.steby.CoreProject.bll.domains.auth.exceptions.PasswordChangeRequiredException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.TwoFactorCodeDeliveryException;
import be.steby.CoreProject.bll.common.exceptions.CoreProjectException;
import be.steby.CoreProject.bll.common.exceptions.RateLimitExceededException;
import be.steby.CoreProject.bll.domains.password.exceptions.InvalidPasswordResetTokenException;
import be.steby.CoreProject.bll.domains.password.services.cookies.PasswordCookieService;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the application.
 *
 * <p>All error responses share a consistent structure:
 * <pre>{@code
 * {
 *   "status":    403,
 *   "errorCode": "FORBIDDEN",
 *   "exception": "AdminPrivilegeException",
 *   "message":   "You do not have the required privileges.",
 *   "timestamp": "2025-03-01T14:30:00"          // always present
 *   // optional extra fields depending on the exception type
 * }
 * }</pre>
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class ControllerAdvisor {

    @Value("${url.back_server}")
    private String BACK_URL;

    private final PasswordCookieService passwordCookieService;

    // =========================================================================
    // Helper
    // =========================================================================

    /**
     * Builds the standard error response body shared by all handlers.
     *
     * @param status    HTTP status code
     * @param errorCode machine-readable error identifier (e.g. {@code "INVALID_TOKEN"})
     * @param exception simple class name of the thrown exception
     * @param message   human-readable error description
     * @return a mutable {@link Map} that handlers can enrich with extra fields
     */
    private Map<String, Object> buildError(int status, String errorCode, String exception, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status",    status);
        body.put("errorCode", errorCode);
        body.put("exception", exception);
        body.put("message",   message);
        body.put("timestamp", LocalDateTime.now());
        return body;
    }

    // =========================================================================
    // CoreProjectException — catch-all for the whole hierarchy
    // =========================================================================

    /**
     * Handles any {@link CoreProjectException} (and all its subclasses that are
     * not handled by a more specific handler below).
     *
     * <p>The {@code errorCode} is read directly from the exception, so each leaf
     * exception controls its own code (e.g. {@code "INVALID_TOKEN"}, {@code "USER_NOT_FOUND"}).
     */
    @ExceptionHandler(CoreProjectException.class)
    public ResponseEntity<Map<String, Object>> handleCoreProjectException(CoreProjectException ex) {
        log.error("Exception occurred: {}", ex.toString());

        Map<String, Object> body = buildError(
                ex.getStatus(),
                ex.getErrorCode(),
                ex.getClass().getSimpleName(),
                ex.getMessage()
        );

        return ResponseEntity
                .status(ex.getStatus())
                .header("Content-Type", "application/json")
                .body(body);
    }

    // =========================================================================
    // Specific handlers (take priority over the catch-all above)
    // =========================================================================

    /**
     * Handles {@link PasswordChangeRequiredException}.
     * Adds a {@code redirectUrl} hint so the frontend knows where to send the user.
     */
    @ExceptionHandler(PasswordChangeRequiredException.class)
    public ResponseEntity<Map<String, Object>> handlePasswordChangeRequired(
            PasswordChangeRequiredException ex,
            HttpServletRequest request) {

        log.warn("Password change required for user accessing: {}", request.getRequestURI());

        Map<String, Object> body = buildError(
                HttpStatus.FORBIDDEN.value(),
                "PASSWORD_CHANGE_REQUIRED",
                ex.getClass().getSimpleName(),
                ex.getMessage()
        );
        body.put("redirectUrl", "/auth/change-password?forced=true");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * Handles bean-validation errors ({@link MethodArgumentNotValidException}).
     * The {@code fieldErrors} list contains one entry per invalid field.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {

        List<String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        List<String> globalErrors = ex.getBindingResult().getGlobalErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();

        Map<String, Object> body = buildError(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                ex.getClass().getSimpleName(),
                "One or more fields failed validation."
        );
        body.put("fieldErrors",  fieldErrors);
        body.put("globalErrors", globalErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles malformed JSON requests, with special treatment for XSS attempts.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {

        Throwable cause = ex.getCause();
        if (cause instanceof JsonMappingException jsonEx) {
            Throwable root = jsonEx.getCause();
            if (root instanceof IllegalArgumentException
                    && root.getMessage() != null
                    && (root.getMessage().contains("HTML") || root.getMessage().contains("Invalid content"))) {

                log.warn("XSS attempt blocked: {}", root.getMessage());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        buildError(HttpStatus.BAD_REQUEST.value(), "XSS_DETECTED",
                                ex.getClass().getSimpleName(), root.getMessage())
                );
            }
        }

        log.error("Invalid request format: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildError(HttpStatus.BAD_REQUEST.value(), "INVALID_FORMAT",
                        ex.getClass().getSimpleName(), "Invalid request format. Please check your JSON data.")
        );
    }

    /**
     * Handles {@link TwoFactorCodeDeliveryException}.
     * Adds the failed delivery method and available alternatives.
     */
    @ExceptionHandler(TwoFactorCodeDeliveryException.class)
    public ResponseEntity<Map<String, Object>> handleTwoFactorCodeDelivery(TwoFactorCodeDeliveryException ex) {

        log.warn("2FA code delivery failed for method {}: {} — Alternatives: {}",
                ex.getFailedMethod(), ex.getMessage(), ex.getAlternativeMethods());

        Map<String, Object> body = buildError(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "2FA_DELIVERY_FAILED",
                ex.getClass().getSimpleName(),
                ex.getMessage()
        );
        body.put("failedMethod",       ex.getFailedMethod().name());
        body.put("alternativeMethods", ex.getAlternativeMethods().stream().map(Enum::name).toList());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    /**
     * Handles {@link RateLimitExceededException}.
     * Adds a {@code Retry-After} header and {@code retryAfter} field (seconds) when available.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(
            RateLimitExceededException ex,
            HttpServletRequest request) {

        log.warn("Rate limit exceeded — URI: {} | IP: {}", request.getRequestURI(), request.getRemoteAddr());

        Map<String, Object> body = buildError(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "RATE_LIMIT_EXCEEDED",
                ex.getClass().getSimpleName(),
                ex.getMessage()
        );

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);

        if (ex.getUnlockTime() != null) {
            long seconds = Math.max(0, ChronoUnit.SECONDS.between(Instant.now(), ex.getUnlockTime()));
            builder.header("Retry-After", String.valueOf(seconds));
            body.put("retryAfter", seconds);
        }

        return builder.body(body);
    }

    // =========================================================================
    // PASSWORD RESET — cookie lifecycle
    // =========================================================================

    /**
     * Handles invalid or expired password-reset permission tokens.
     * Clears the {@code password_reset_permission} cookie since it is unrecoverable.
     */
    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPasswordResetToken(
            InvalidPasswordResetTokenException ex,
            HttpServletResponse response) {

        log.warn("Invalid password reset permission token: {}", ex.getMessage());
        passwordCookieService.clearPasswordResetPermissionCookie(response);

        Map<String, Object> body = buildError(
                HttpStatus.UNAUTHORIZED.value(),
                "PERMISSION_EXPIRED",
                ex.getClass().getSimpleName(),
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header("Content-Type", "application/json")
                .body(body);
    }
}