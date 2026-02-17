package be.steby.CoreProject.pl.advisor;

import be.steby.CoreProject.bll.domains.auth.exceptions.PasswordChangeRequiredException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.TwoFactorCodeDeliveryException;
import be.steby.CoreProject.bll.exceptions.CoreProjectException;
import be.steby.CoreProject.bll.exceptions.RateLimitExceededException;
import be.steby.CoreProject.bll.exceptions.TokenExpiredException;
import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;
import be.steby.CoreProject.dl.entities.tokens.BaseToken;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import jakarta.servlet.http.HttpServletRequest;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller advice class for global exception handling in the application.
 * Uses {@code @ControllerAdvice} to capture and handle specific exceptions in a centralized manner.
 */
@Slf4j
@ControllerAdvice
public class ControllerAdvisor {

    @Value("${url.back_server}")
    private String BACK_URL;

    /**
     * <p>Handles exceptions of type {@link CoreProjectException}.</p>
     * <p>This method captures {@link CoreProjectException}, logs the error message,
     * and returns an HTTP response with a status containing the error message.</p>
     *
     * @param error the {@link CoreProjectException} that was thrown
     * @return a {@link ResponseEntity} containing the error message with a status
     */
    @ExceptionHandler(CoreProjectException.class)
    public ResponseEntity<Map<String, String>> handleCoreProjectException(CoreProjectException error) {
        log.error("Exception occurred: {}", error.toString());
        HashMap<String, String> map = new HashMap<>();
        map.put("error", error.getMessage());
        return ResponseEntity.status(error.getStatus())
                .header("Content-Type", "application/json")
                .body(map);
    }


    @ExceptionHandler(PasswordChangeRequiredException.class)
    public ResponseEntity<Map<String, Object>> handlePasswordChangeRequired(
            PasswordChangeRequiredException ex,
            HttpServletRequest request) {

        log.warn("Password change required for user accessing: {}", request.getRequestURI());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "PASSWORD_CHANGE_REQUIRED");
        response.put("message", ex.getMessage());
        response.put("redirectUrl", "/auth/change-password?forced=true");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handles method argument validation exceptions.
     * {@link MethodArgumentNotValidException}
     *
     * @param error The {@code MethodArgumentNotValidException} to handle.
     * @return A {@code ResponseEntity} with a status code of 406 (Not Acceptable) and a body containing a list of validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<String>>> handleValidationErrors(MethodArgumentNotValidException error) {
        Map<String, List<String>> errorResponse = new HashMap<>();
        errorResponse.put("errors", error.getBindingResult().getFieldErrors()
                .stream().map(FieldError::getDefaultMessage).collect(Collectors.toList()));
        errorResponse.get("errors").addAll(
                error.getBindingResult().getGlobalErrors()
                        .stream()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .toList()
        );
        errorResponse.put("globalErrors", error.getBindingResult().getGlobalErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.toList()));
        return ResponseEntity.status(400).body(errorResponse);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadable(HttpMessageNotReadableException error) {
        log.error("Invalid request format: {}", error.getMessage());

        Map<String, String> response = new HashMap<>();
        response.put("error", "Invalid request format. Please check your JSON data.");

        return ResponseEntity.status(400) // Bad Request
                .header("Content-Type", "application/json")
                .body(response);
    }


    /**
     * Handles TwoFactorCodeDeliveryException when 2FA code delivery fails.
     *
     * Returns 503 Service Unavailable with:
     * - Error message explaining the failure
     * - The method that failed
     * - Available alternative methods
     *
     * @param error the TwoFactorCodeDeliveryException
     * @return ResponseEntity with 503 status and alternatives
     */
    @ExceptionHandler(TwoFactorCodeDeliveryException.class)
    public ResponseEntity<Map<String, Object>> handleTwoFactorCodeDeliveryException(
            TwoFactorCodeDeliveryException error) {

        log.warn("2FA code delivery failed for method {}: {} - Alternatives: {}",
                error.getFailedMethod(), error.getMessage(), error.getAlternativeMethods());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "2FA_DELIVERY_FAILED");
        response.put("message", error.getMessage());
        response.put("failedMethod", error.getFailedMethod().name());
        response.put("alternativeMethods", error.getAlternativeMethods().stream()
                .map(Enum::name)
                .toList());
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Content-Type", "application/json")
                .body(response);
    }


    /**
     * Handles rate limit exceeded exceptions when users make too many requests.
     *
     * <p>Returns HTTP 429 (Too Many Requests) with:</p>
     * <ul>
     *   <li>Error code indicating rate limit exceeded</li>
     *   <li>User-friendly message</li>
     *   <li>Retry-After header (seconds to wait)</li>
     *   <li>Timestamp of when the error occurred</li>
     * </ul>
     *
     * <h4>Example Response:</h4>
     * <pre>{@code
     * HTTP/1.1 429 Too Many Requests
     * Retry-After: 45
     * Content-Type: application/json
     *
     * {
     *   "error": "RATE_LIMIT_EXCEEDED",
     *   "message": "Too many requests. Please try again later.",
     *   "timestamp": "2025-02-17T14:30:00"
     * }
     * }</pre>
     *
     * @param ex the RateLimitExceededException that was thrown
     * @param request the HTTP request that triggered the rate limit
     * @return ResponseEntity with 429 status and error details
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(
            RateLimitExceededException ex,
            HttpServletRequest request) {

        log.warn("Rate limit exceeded for request: {} from IP: {}",
                request.getRequestURI(),
                request.getRemoteAddr());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "RATE_LIMIT_EXCEEDED");
        response.put("message", ex.getMessage());
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Content-Type", "application/json")
                .body(response);
    }
}

