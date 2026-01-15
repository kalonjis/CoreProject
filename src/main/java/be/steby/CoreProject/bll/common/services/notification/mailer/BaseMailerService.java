package be.steby.CoreProject.bll.common.services.notification.mailer;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.il.mail.EmailComposer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.thymeleaf.context.Context;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Base class for all domain-specific mailer services.
 * Provides common utilities for email sending such as username formatting,
 * URL building, and time formatting.
 */
@RequiredArgsConstructor
public abstract class BaseMailerService {

    protected final EmailComposer emailComposer;

    @Value("${url.front_server}")
    protected String frontUrl;

    /**
     * Defines the username to display in emails.
     * Uses firstname if available, otherwise falls back to username.
     *
     * @param user The user
     * @return The display name
     */
    protected String defineUsername(User user) {
        return user.getFirstname() != null ? user.getFirstname() : user.getUsername();
    }

    /**
     * Formats an Instant into a human-readable relative time string.
     *
     * @param instant The instant to format
     * @return A formatted string like "2 hours ago"
     */
    protected String formatTime(Instant instant) {
        if (instant == null) {
            return "Unknown time";
        }

        Instant now = Instant.now();
        Duration duration = Duration.between(instant, now);
        long seconds = duration.getSeconds();

        if (seconds < 60) {
            return seconds == 1 ? "1 second ago" : seconds + " seconds ago";
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        }

        long days = hours / 24;
        if (days < 7) {
            return days == 1 ? "1 day ago" : days + " days ago";
        }

        if (days < 30) {
            long weeks = days / 7;
            return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
        }

        long months = days / 30;
        return months == 1 ? "1 month ago" : months + " months ago";
    }

    /**
     * Formats an Instant to a readable date string with timezone.
     *
     * @param instant The instant to format
     * @return A formatted date string
     */
    protected String formatDate(Instant instant) {
        if (instant == null) {
            return "Unknown date";
        }

        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("dd MMM yyyy HH:mm:ss z")
                .withZone(ZoneId.systemDefault());

        return formatter.format(instant);
    }

    /**
     * Builds a front-end URL with the given path and optional query parameters.
     *
     * @param path The path (e.g., "/auth/reset-password")
     * @param params Optional query parameters in key-value pairs
     * @return The complete URL
     */
    protected String buildUrl(String path, String... params) {
        StringBuilder url = new StringBuilder(frontUrl).append(path);

        if (params.length > 0) {
            url.append("?");
            for (int i = 0; i < params.length; i += 2) {
                if (i > 0) {
                    url.append("&");
                }
                url.append(params[i]).append("=").append(params[i + 1]);
            }
        }

        return url.toString();
    }

    /**
     * Creates a basic context with username variable.
     *
     * @param user The user
     * @return A context with username set
     */
    protected Context createBaseContext(User user) {
        Context context = new Context();
        context.setVariable("username", defineUsername(user));
        return context;
    }

    /**
     * Sends an email using the mailer utility.
     *
     * @param subject The email subject
     * @param templateName The template name (without "emails/" prefix)
     * @param context The template context
     * @param recipients The recipient email addresses
     */
    protected void sendEmail(String subject, String templateName, Context context, String... recipients) {
        emailComposer.sendMail(subject, templateName, context, recipients);
    }
}